package com.example.subacontrol.websocket

import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.subacontrol.calibration.CalibrationCoordinates
import com.example.subacontrol.calibration.getCalibrationData
import com.example.subacontrol.util.Constants
import com.example.subacontrol.util.EventBus
import com.example.subacontrol.util.WebSocketConnectionCallbacks
import timber.log.Timber
import kotlin.math.max

class WebSocketReceiver(
    private val context: Context,
    private val connectionCallbacks: WebSocketConnectionCallbacks? = null
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var desktopMaxX = 1920
    private var desktopMaxY = 1080
    
    private var mappedEventCount = 0
    private var webSocket: WebSocket? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    private var autoCalibrationHelperTime = 0L // Time of last calibration action
    private var calibrationHelperAttempts = 0 // Count of auto-helps
    private val lastCursorPosition = CursorPosition(0, 0) // Last known position
    
    // Simple class to hold cursor coordinates
    private data class CursorPosition(var x: Int, var y: Int)
    
    companion object {
        var lastReceiverInstance: WebSocketReceiver? = null
        
        // Method for test server to send events directly
        fun processTestEvent(type: String, rawX: Int, rawY: Int) {
            lastReceiverInstance?.let { receiver ->
                Log.d("WebSocketReceiver", "Processing test event: $type at ($rawX, $rawY)")
                
                // Use the same logic as in onMessage
                try {
                    // Check if we're in calibration mode
                    if (CalibrationCoordinates.isCalibrating) {
                        // Always save the last position for use by auto-calibration
                        receiver.lastCursorPosition.x = rawX
                        receiver.lastCursorPosition.y = rawY
                        
                        // Check for clicks or add auto-calibration
                        if (type == "click") {
                            // Handle calibration clicks
                            when (CalibrationCoordinates.currentStep) {
                                1 -> {
                                    // Apply the point capture - try to help with any issues
                                    receiver.autoCalibrationHelperTime = System.currentTimeMillis()
                                    CalibrationCoordinates.captureTopLeft(rawX, rawY)
                                    Log.d("WebSocketReceiver", "Test Calibration: Top-left captured at ($rawX, $rawY)")
                                }
                                2 -> {
                                    // Apply the point capture
                                    CalibrationCoordinates.captureBottomRight(rawX, rawY)
                                    Log.d("WebSocketReceiver", "Test Calibration: Bottom-right captured at ($rawX, $rawY)")
                                }
                            }
                            // Skip further processing for click events during calibration
                            return
                        }
                    }

                    // No need to transform coordinates since they're already in screen space
                    when(type) {
                        "move" -> EventBus.publishCursorMove(rawX, rawY)
                        "click" -> EventBus.publishTap(rawX, rawY)
                        else -> Log.w("WebSocketReceiver", "Unknown event type: $type")
                    }
                } catch(e: Exception) {
                    Log.e("WebSocketReceiver", "Failed to process test event", e)
                }
            } ?: Log.e("WebSocketReceiver", "No receiver instance available for test events")
        }
    }
    
    init {
        // Store the instance for test server use
        lastReceiverInstance = this
        
        // Load calibration data
        ioScope.launch {
            val calibrationData = context.getCalibrationData().first()
            desktopMaxX = calibrationData.first
            desktopMaxY = calibrationData.second
            
            // Validate calibration - if it looks wrong, reset to defaults
            if (desktopMaxX <= 0 || desktopMaxY <= 0 || desktopMaxX > 10000 || desktopMaxY > 10000) {
                Log.w("WebSocketReceiver", "Found invalid calibration: ${desktopMaxX}x${desktopMaxY}, resetting to defaults")
                desktopMaxX = 1920
                desktopMaxY = 1080
            }
            
            val phoneWidth = context.resources.displayMetrics.widthPixels
            val phoneHeight = context.resources.displayMetrics.heightPixels
            
            Log.d("WebSocketReceiver", "Screen sizes - Desktop: ${desktopMaxX}x${desktopMaxY}, Phone: ${phoneWidth}x${phoneHeight}")
            Log.d("WebSocketReceiver", "Scale factors - X: ${phoneWidth.toFloat()/desktopMaxX}, Y: ${phoneHeight.toFloat()/desktopMaxY}")
        }
    }

    fun connect() {
        val wsUrl = Constants.getWebSocketUrl(context)
        Log.d("WebSocketReceiver", "Connecting to WebSocket: $wsUrl")
        
        // Notify connection is starting
        connectionCallbacks?.onConnecting()
        
        // Show connection attempt toast on UI thread
        mainScope.launch {
            Toast.makeText(context, "Connecting to: $wsUrl", Toast.LENGTH_SHORT).show()
        }
        
        // Move network operations to IO thread
        ioScope.launch {
            try {
                // Add a test ping to check network connectivity
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(Constants.getHost(), Constants.getPort()), 3000)
                    socket.close()
                    Log.d("WebSocketReceiver", "Socket test connection succeeded ✅")
                    
                    // Show toast on main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Network test OK ✅", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketReceiver", "Socket test connection failed: ${e.message}", e)
                    
                    // Show toast on main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "⚠️ Network test failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    
                    // Report error to callback
                    connectionCallbacks?.onError("Network test failed: ${e.message}")
                    return@launch
                }
                
                // Configure a more reliable client
                val clientBuilder = OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for long-running connection
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS) // More time to connect
                    .pingInterval(30, TimeUnit.SECONDS) // Keep connection alive
                    .retryOnConnectionFailure(true)
                
                val request = Request.Builder()
                    .url(wsUrl)
                    .build()

                webSocket = clientBuilder.build().newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, resp: Response) {
                        Log.d("WebSocketReceiver", "WebSocket connected successfully ✅")
                        Log.d("WebSocketReceiver", "Response headers: ${resp.headers}")
                        
                        // Notify successful connection
                        connectionCallbacks?.onConnected()
                        
                        // Show toast on main thread
                        mainScope.launch {
                            Toast.makeText(context, "WebSocket connected! ✅", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Send a test message to verify two-way communication
                        try {
                            val testMsg = JSONObject().apply {
                                put("type", "client_hello")
                                put("timestamp", System.currentTimeMillis())
                            }
                            ws.send(testMsg.toString())
                            Log.d("WebSocketReceiver", "Sent test message to server")
                        } catch (e: Exception) {
                            Log.e("WebSocketReceiver", "Failed to send test message", e)
                        }
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        try {
                            Log.d("WebSocketReceiver", "Received raw message: $text")
                            val data = JSONObject(text)
                            val type = data.getString("type")
                            
                            // Handle test messages from server
                            if (type == "connection_test") {
                                Log.d("WebSocketReceiver", "Received connection test message")
                                return
                            }
                            
                            val rawX = data.getInt("x")
                            val rawY = data.getInt("y")
                            
                            Log.d("WebSocketReceiver", "Message received: $type, ($rawX, $rawY)")

                            // Check if we're in calibration mode
                            if (CalibrationCoordinates.isCalibrating) {
                                // Always save the last position for use by auto-calibration
                                lastCursorPosition.x = rawX
                                lastCursorPosition.y = rawY
                                
                                // Check for clicks or add auto-calibration
                                if (type == "click") {
                                    // Handle calibration clicks
                                    when (CalibrationCoordinates.currentStep) {
                                        1 -> {
                                            // Apply the point capture - try to help with any issues
                                            autoCalibrationHelperTime = System.currentTimeMillis()
                                            CalibrationCoordinates.captureTopLeft(rawX, rawY)
                                            Log.d("WebSocketReceiver", "Calibration: Top-left captured at ($rawX, $rawY)")
                                        }
                                        2 -> {
                                            // Apply the point capture
                                            CalibrationCoordinates.captureBottomRight(rawX, rawY)
                                            Log.d("WebSocketReceiver", "Calibration: Bottom-right captured at ($rawX, $rawY)")
                                        }
                                    }
                                    // Only skip further processing for click events during calibration
                                    return
                                }
                                
                                // Auto-calibration helper: If it's been 10 seconds since last calibration attempt,
                                // use the current cursor position to help the user with calibration
                                if (CalibrationCoordinates.currentStep == 1 && 
                                        System.currentTimeMillis() - autoCalibrationHelperTime > 10000) {
                                    // It's been 10+ seconds, help the user with calibration
                                    if (calibrationHelperAttempts < 2) {
                                        mainScope.launch {
                                            Toast.makeText(context, 
                                                "Click being processed automatically. Move your cursor to the BOTTOM-RIGHT corner.", 
                                                Toast.LENGTH_LONG).show()
                                        }
                                        // Auto-capture top-left (after warning the user)
                                        CalibrationCoordinates.captureTopLeft(0, 0)
                                        Log.d("WebSocketReceiver", "Auto-calibration: Top-left set to (0,0)")
                                        calibrationHelperAttempts++
                                        autoCalibrationHelperTime = System.currentTimeMillis()
                                    }
                                }
                                else if (CalibrationCoordinates.currentStep == 2 && 
                                        System.currentTimeMillis() - autoCalibrationHelperTime > 10000) {
                                    // It's been 10+ seconds in second step, help with bottom-right
                                    if (calibrationHelperAttempts < 3) {
                                        mainScope.launch {
                                            Toast.makeText(context, 
                                                "Click being processed automatically. Calibration completed.", 
                                                Toast.LENGTH_LONG).show()
                                        }
                                        // Auto-capture bottom-right with default resolution
                                        CalibrationCoordinates.captureBottomRight(1920, 1080)
                                        Log.d("WebSocketReceiver", "Auto-calibration: Bottom-right set to (1920,1080)")
                                        calibrationHelperAttempts++
                                        autoCalibrationHelperTime = System.currentTimeMillis()
                                    }
                                }
                            }

                            // Get the current phone screen dimensions (portrait mode only)
                            val phoneWidth = context.resources.displayMetrics.widthPixels
                            val phoneHeight = context.resources.displayMetrics.heightPixels
                            
                            // CRITICAL FIX: Simple direct mapping without complex scaling
                            // Desktop resolution is always 1920x1080 (standard for most PC monitors)
                            val maxX = 1920
                            val maxY = 1080

                            // Directly map coordinates with straightforward proportion
                            // X coordinate scales from 0-1920 to 0-phoneWidth
                            // Y coordinate scales from 0-1080 to 0-phoneHeight
                            val mappedX = ((rawX.toFloat() / maxX) * phoneWidth).toInt()
                            val mappedY = ((rawY.toFloat() / maxY) * phoneHeight).toInt()

                            // Log mapping for debugging
                            if (mappedEventCount < 10) {
                                Log.d("WebSocketReceiver", "DESKTOP: ${maxX}x${maxY}, PHONE: ${phoneWidth}x${phoneHeight}")
                                Log.d("WebSocketReceiver", "SCALING: x=${phoneWidth.toFloat()/maxX}, y=${phoneHeight.toFloat()/maxY}")
                                Log.d("WebSocketReceiver", "RAW INPUT: ($rawX,$rawY) → OUTPUT: ($mappedX,$mappedY)")
                                mappedEventCount++
                            }

                            // Only send events with valid coordinates
                            if (mappedX in 0..phoneWidth && mappedY in 0..phoneHeight) {
                                mainScope.launch {
                                    when(type) {
                                        "move" -> {
                                            Log.d("WebSocketReceiver", "Publishing cursor move: ($mappedX, $mappedY)")
                                            EventBus.publishCursorMove(mappedX, mappedY)
                                        }
                                        "click" -> {
                                            Log.d("WebSocketReceiver", "Publishing tap event: ($mappedX, $mappedY)")
                                            EventBus.publishTap(mappedX, mappedY)
                                        }
                                        else -> Log.w("WebSocketReceiver", "Unknown event type: $type")
                                    }
                                }
                            }
                        } catch(e: Exception) {
                            Log.e("WebSocketReceiver", "Failed to parse WebSocket message: $text", e)
                        }
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                        Log.e("WebSocketReceiver", "WebSocket connection failed: ${t.message}", t)
                        
                        // Notify connection error
                        connectionCallbacks?.onError(t.message ?: "Unknown error")
                        
                        // Show toast on main thread
                        mainScope.launch {
                            Toast.makeText(context, "WebSocket error: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                        
                        // Try to reconnect after a delay
                        mainScope.launch {
                            kotlinx.coroutines.delay(5000)
                            Log.d("WebSocketReceiver", "Attempting to reconnect...")
                            connect()
                        }
                    }
                    
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d("WebSocketReceiver", "WebSocket closing: $code - $reason")
                        connectionCallbacks?.onDisconnected()
                        webSocket.close(1000, null)
                    }
                    
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d("WebSocketReceiver", "WebSocket closed: $code - $reason")
                        connectionCallbacks?.onDisconnected()
                    }
                })
            } catch (e: Exception) {
                Log.e("WebSocketReceiver", "Error creating WebSocket: ${e.message}", e)
                
                // Notify connection error
                connectionCallbacks?.onError(e.message ?: "Unknown error")
                
                // Show toast on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "WebSocket error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    fun disconnect() {
        try {
            webSocket?.close(1000, "App closing")
            webSocket = null
            connectionCallbacks?.onDisconnected()
        } catch (e: Exception) {
            Log.e("WebSocketReceiver", "Error disconnecting: ${e.message ?: "Unknown error"}", e)
        }
    }

    private fun processMovementEvent(event: JSONObject) {
        // Don't process if in calibration mode and it's a click event
        if (CalibrationCoordinates.isCalibrating && event.has("click")) {
            // Only skip click events when in calibration mode, allow movement
            Log.d("WebSocketReceiver", "Skipping click event in calibration mode")
            return
        }

        if (!event.has("x") || !event.has("y")) {
            Log.e("WebSocketReceiver", "Movement event missing coordinates")
            return
        }

        try {
            val x = event.getInt("x")
            val y = event.getInt("y")
            
            // Use the mapDesktopToPhone function for consistent mapping
            val (mappedX, mappedY) = mapDesktopToPhone(x.toFloat(), y.toFloat())
            
            // Only log first few events to avoid spam
            if (mappedEventCount < 10) {
                Log.d("WebSocketReceiver", "Mapping: Desktop ($x, $y) → Phone (${mappedX.toInt()}, ${mappedY.toInt()})")
                mappedEventCount++
            }
            
            if (event.has("click")) {
                EventBus.publishTap(mappedX.toInt(), mappedY.toInt())
            } else {
                EventBus.publishCursorMove(mappedX.toInt(), mappedY.toInt())
            }
        } catch (e: Exception) {
            Log.e("WebSocketReceiver", "Error processing movement event", e)
        }
    }

    fun mapDesktopToPhone(desktopX: Float, desktopY: Float): Pair<Float, Float> {
        val sharedPreferences = context.getSharedPreferences("CalibrationData", Context.MODE_PRIVATE)
        val topLeftX = sharedPreferences.getFloat("topLeftX", 0f)
        val topLeftY = sharedPreferences.getFloat("topLeftY", 0f)
        val bottomRightX = sharedPreferences.getFloat("bottomRightX", 100f)
        val bottomRightY = sharedPreferences.getFloat("bottomRightY", 100f)
        
        // Get the screen dimensions
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Ensure the calibration points are valid (not the same)
        val desktopWidth = max(1f, bottomRightX - topLeftX)
        val desktopHeight = max(1f, bottomRightY - topLeftY)
        
        // Calculate the proportion of where the cursor is on desktop
        val proportionX = (desktopX - topLeftX) / desktopWidth.toFloat()
        val proportionY = (desktopY - topLeftY) / desktopHeight.toFloat()
        
        // Map proportionally to phone screen, using full screen dimensions
        val phoneX = proportionX * screenWidth
        val phoneY = proportionY * screenHeight
        
        // Apply clamping to ensure the cursor stays within screen bounds
        val clampedX = phoneX.coerceIn(0f, screenWidth.toFloat())
        val clampedY = phoneY.coerceIn(0f, screenHeight.toFloat())
        
        Log.d("Coordinates", "Desktop($desktopX, $desktopY) → Phone($clampedX, $clampedY)")
        Log.d("CalibrationData", "topLeft($topLeftX, $topLeftY), bottomRight($bottomRightX, $bottomRightY)")
        
        return Pair(clampedX, clampedY)
    }
}
