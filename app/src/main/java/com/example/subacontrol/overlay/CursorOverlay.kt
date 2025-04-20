package com.example.subacontrol.overlay

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.subacontrol.accessibility.TapAccessibilityService
import com.example.subacontrol.util.EventBus
import com.example.subacontrol.util.events.CursorMoveEvent
import com.example.subacontrol.util.events.TapEvent
import com.example.subacontrol.websocket.WebSocketReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.example.subacontrol.ui.theme.SubaColors

/**
 * Material-inspired cursor overlay service
 * Maintains a persistent connection and responsive cursor even when app is in foreground
 */
class CursorOverlay : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var cursorView: MaterialCursorView
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Store WebSocketReceiver instance to manage lifecycle
    private var webSocketReceiver: WebSocketReceiver? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SubaControlOverlayChannel"
        
        // Cursor appearance constants - updated to Subaru palette with larger size
        const val CURSOR_SIZE_DP = 32 // Increased from 24 to 32
        // Subaru Navy at 80% opacity (0xCC1F3057) as two's complement signed int
        const val CURSOR_PRIMARY_COLOR = -0x33E0CFA9 // Subaru Navy at 80% opacity
        const val CURSOR_SHADOW_COLOR = 0x55000000 // Increased shadow opacity for better visibility
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("CursorOverlay", "Service onCreate starting")
        
        try {
            // Start foreground service with notification
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("CursorOverlay", "Started as foreground service")
            
            // Get screen dimensions for logging
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            Log.d("CursorOverlay", "Screen dimensions: ${screenWidth}x${screenHeight}")
            
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Create material cursor view
            cursorView = MaterialCursorView(this)
            
            // Convert dp to pixels for sizing
            val density = resources.displayMetrics.density
            val cursorSizePx = (CURSOR_SIZE_DP * density).toInt()
            
            // Set up window parameters with appropriate flags to stay responsive
            layoutParams = WindowManager.LayoutParams(
                cursorSizePx, 
                cursorSizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Critical flags to ensure cursor stays responsive even in foreground
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                // Start position at center of screen
                x = metrics.widthPixels / 2 - cursorSizePx / 2
                y = metrics.heightPixels / 2 - cursorSizePx / 2
            }
            
            // Add view to window
            windowManager.addView(cursorView, layoutParams)
            Log.d("CursorOverlay", "Overlay view added successfully")
            
            // Show toast for visibility
            Toast.makeText(this, "Cursor overlay started", Toast.LENGTH_SHORT).show()

            // Subscribe to cursor/tap events
            Log.d("CursorOverlay", "Subscribing to EventBus events")

            // Make sure we're collecting events on the main thread
            val mainScope = CoroutineScope(Dispatchers.Main)
            mainScope.launch {
                Log.d("CursorOverlay", "Started event collection on main thread")
                EventBus.events
                    .onEach { e ->
                        try {
                            when(e) {
                                is CursorMoveEvent -> {
                                    Log.d("CursorOverlay", "Move event received: (${e.x}, ${e.y})")
                                    // Check if view is null or window token lost
                                    if (cursorView.isAttachedToWindow) {
                                        // Use a more resilient approach to update layout
                                        try {
                                            // Get current screen dimensions
                                            val screenWidth = resources.displayMetrics.widthPixels
                                            val screenHeight = resources.displayMetrics.heightPixels
                                            
                                            // IMPROVED: Use direct positioning without complex constraints
                                            // Just ensure cursor stays within screen bounds
                                            val safeX = e.x.coerceIn(0, screenWidth - 1)
                                            val safeY = e.y.coerceIn(0, screenHeight - 1)
                                            
                                            // Log if we had to adjust the coordinates significantly
                                            if (Math.abs(e.x - safeX) > 50 || Math.abs(e.y - safeY) > 50) {
                                                Log.w("CursorOverlay", "Position adjustment: (${e.x},${e.y}) → ($safeX,$safeY)")
                                            }
                                            
                                            // Update layout with valid coordinates
                                            layoutParams.x = safeX
                                            layoutParams.y = safeY
                                            
                                            try {
                                                windowManager.updateViewLayout(cursorView, layoutParams)
                                            } catch (ex: IllegalArgumentException) {
                                                Log.e("CursorOverlay", "Failed to update layout: ${ex.message}")
                                                recreateOverlayView()
                                            }
                                        } catch (ex: Exception) {
                                            Log.e("CursorOverlay", "Failed to update layout: ${ex.message}", ex)
                                            // Try to recreate the view if it's not working
                                            recreateOverlayView()
                                        }
                                    } else {
                                        Log.e("CursorOverlay", "View not attached to window, recreating")
                                        recreateOverlayView()
                                    }
                                }
                                is TapEvent -> {
                                    Log.d("CursorOverlay", "Tap event received at (${e.x}, ${e.y})")
                                    cursorView.animateClick()
                                    TapAccessibilityService.instance?.simulateTap(e.x, e.y)
                                }
                            }
                        } catch (ex: Exception) {
                            Log.e("CursorOverlay", "Error processing event: ${ex.message}", ex)
                            // Try to recover
                            recreateOverlayView()
                        }
                    }
                    .launchIn(scope)
            }
            
            Log.d("CursorOverlay", "Service started successfully")

            // Initialize WebSocket connection if not already
            if (webSocketReceiver == null) {
                Log.d("CursorOverlay", "Creating WebSocketReceiver")
                webSocketReceiver = WebSocketReceiver(this)
                webSocketReceiver?.connect()
            }
        } catch (e: Exception) {
            Log.e("CursorOverlay", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CursorOverlay", "onStartCommand called")
        
        // Handle restart action if provided
        if (intent?.action == "RESTART_CURSOR_OVERLAY") {
            Log.d("CursorOverlay", "Explicit restart requested, reconnecting WebSocket")
            // Recreate the websocket connection if needed
            if (webSocketReceiver == null) {
                webSocketReceiver = WebSocketReceiver(this)
                webSocketReceiver?.connect()
            }
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SubaControl Cursor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for SubaControl cursor overlay"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SubaControl Active")
        .setContentText("Mouse cursor control is active")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        try {
            webSocketReceiver?.disconnect()
            webSocketReceiver = null
            
            windowManager.removeView(cursorView)
            scope.cancel()
            Log.d("CursorOverlay", "Service destroyed")
        } catch (e: Exception) {
            Log.e("CursorOverlay", "Error in onDestroy", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun recreateOverlayView() {
        try {
            if (::cursorView.isInitialized) {
                windowManager.removeView(cursorView)
            }
            // Create a new coroutine scope for this function instead of using mainScope
            CoroutineScope(Dispatchers.Main).launch {
                createOverlayView()
            }
        } catch (e: Exception) {
            Log.e("CursorOverlay", "Error recreating overlay view: ${e.message}")
        }
    }

    private fun createOverlayView() {
        try {
            Log.d("CursorOverlay", "Recreating overlay view")
            
            // Get screen dimensions
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            
            // Create and add new view
            cursorView = MaterialCursorView(this)
            val density = resources.displayMetrics.density
            val cursorSizePx = (CURSOR_SIZE_DP * density).toInt()
            
            // Reset layout params
            layoutParams = WindowManager.LayoutParams(
                cursorSizePx, 
                cursorSizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                // Start position at center of screen
                x = screenWidth / 2 - cursorSizePx / 2
                y = screenHeight / 2 - cursorSizePx / 2
            }
            
            try {
                windowManager.addView(cursorView, layoutParams)
                Log.d("CursorOverlay", "Overlay view recreated at position (${layoutParams.x}, ${layoutParams.y})")
                
                // Show a toast on UI thread - use proper coroutine scope
                val toastScope = CoroutineScope(Dispatchers.Main)
                toastScope.launch {
                    Toast.makeText(this@CursorOverlay, "Cursor restored at center", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CursorOverlay", "Failed to add overlay view: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e("CursorOverlay", "Failed to recreate overlay: ${e.message}", e)
        }
    }
}

/**
 * Custom Material Design inspired cursor view
 */
@SuppressLint("ViewConstructor")
class MaterialCursorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = CursorOverlay.CURSOR_PRIMARY_COLOR
        setShadowLayer(6f, 0f, 3f, CursorOverlay.CURSOR_SHADOW_COLOR)
    }
    
    private val cursorPath = Path()
    private var isClicking = false
    private var clickAnimationStartTime = 0L
    
    init {
        // Set hardware acceleration for smoother rendering
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Create cursor shape
        updateCursorPath()
    }
    
    private fun updateCursorPath() {
        val w = resources.displayMetrics.density * CursorOverlay.Companion.CURSOR_SIZE_DP
        val h = w
        
        cursorPath.reset()
        // Create pointer triangle shape - slightly tweaked proportions
        cursorPath.moveTo(0f, 0f)
        cursorPath.lineTo(w, h/2)
        cursorPath.lineTo(0f, h)
        // Add a small tail for better visibility
        cursorPath.lineTo(w/4, h/2)
        cursorPath.close()
    }
    
    fun animateClick() {
        isClicking = true
        clickAnimationStartTime = System.currentTimeMillis()
        invalidate()
        
        // Reset state after animation
        postDelayed({
            isClicking = false
            invalidate()
        }, 300)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply scaling effect during click
        if (isClicking) {
            val elapsed = System.currentTimeMillis() - clickAnimationStartTime
            val scale = if (elapsed < 150) 0.8f else 1.0f
            
            canvas.save()
            canvas.scale(scale, scale, width/2f, height/2f)
        }
        
        // Draw cursor
        canvas.drawPath(cursorPath, paint)
        
        if (isClicking) {
            canvas.restore()
        }
    }
} 