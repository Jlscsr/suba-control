package com.example.subacontrol.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * A test class to simulate mouse events directly on the device
 * This helps us test the app without needing a laptop connection
 */
class TestServer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var isRunning = false
    
    // Get device width and height for mouse movement simulation
    private val screenWidth = context.resources.displayMetrics.widthPixels
    private val screenHeight = context.resources.displayMetrics.heightPixels
    
    fun start() {
        if (isRunning) return
        
        isRunning = true
        executor.execute {
            // Check if port 8080 is already in use
            if (isPortInUse(8080)) {
                showToast("Port 8080 is already in use. Can't start test server.")
                Log.d("TestServer", "Port 8080 is already in use")
                return@execute
            }
            
            showToast("Starting test server...")
            Log.d("TestServer", "Starting simulation of mouse events")
            
            // Simulate mouse events
            simulateMouseEvents()
        }
    }
    
    private fun simulateMouseEvents() {
        var x = screenWidth / 2
        var y = screenHeight / 2
        
        while (isRunning) {
            // Random direction changes
            x += Random.nextInt(-20, 20)
            y += Random.nextInt(-20, 20)
            
            // Keep cursor within screen bounds
            x = x.coerceIn(0, screenWidth)
            y = y.coerceIn(0, screenHeight)
            
            // Send move event to the EventBus
            val moveEvent = MovementEvent("move", x, y)
            sendEvent(moveEvent)
            
            // Occasionally simulate a click
            if (Random.nextInt(50) == 0) {
                val clickEvent = MovementEvent("click", x, y)
                sendEvent(clickEvent)
                Log.d("TestServer", "Click at ($x, $y)")
            }
            
            try {
                Thread.sleep(50) // 20 fps movement
            } catch (e: InterruptedException) {
                break
            }
        }
    }
    
    private fun sendEvent(event: MovementEvent) {
        // Instead of sending via WebSocket, we'll send directly to the EventBus
        handler.post {
            com.example.subacontrol.websocket.WebSocketReceiver.processTestEvent(event.type, event.x, event.y)
        }
    }
    
    private fun isPortInUse(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("localhost", port), 100)
                true // Port is open and in use
            }
        } catch (e: IOException) {
            false // Port is available
        }
    }
    
    fun stop() {
        isRunning = false
        executor.shutdown()
    }
    
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    data class MovementEvent(val type: String, val x: Int, val y: Int)
} 