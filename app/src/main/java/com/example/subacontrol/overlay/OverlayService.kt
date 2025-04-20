package com.example.subacontrol.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.subacontrol.MainActivity
import com.example.subacontrol.accessibility.TapAccessibilityService
import com.example.subacontrol.websocket.WebSocketReceiver
import com.example.subacontrol.util.EventBus
import com.example.subacontrol.util.events.CursorMoveEvent
import com.example.subacontrol.util.events.TapEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: WindowManager.LayoutParams
    private lateinit var cursorView: View
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Store WebSocketReceiver instance to manage lifecycle
    private var webSocketReceiver: WebSocketReceiver? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SubaControlOverlayChannel"
    }

    override fun onCreate() {
        super.onCreate()
        // Log directly to Android Log since Timber might not be initialized
        Log.d("OverlayService", "Service onCreate starting")
        
        try {
            // Start foreground service with notification
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("OverlayService", "Started as foreground service")
            
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            Log.d("OverlayService", "Got window manager")
            
            // Create cursor view programmatically instead of using XML layout
            val frameLayout = FrameLayout(this)
            frameLayout.layoutParams = FrameLayout.LayoutParams(64, 64)
            frameLayout.setBackgroundColor(android.graphics.Color.BLUE)
            
            val imageView = ImageView(this)
            imageView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Make the cursor bright red
            imageView.setImageDrawable(resources.getDrawable(android.R.drawable.ic_delete, null))
            imageView.setColorFilter(android.graphics.Color.RED)
            frameLayout.addView(imageView)
            
            cursorView = frameLayout
            
            // Debug: Get screen dimensions to position in center
            val metrics = resources.displayMetrics
            val centerX = metrics.widthPixels / 2 - 32
            val centerY = metrics.heightPixels / 2 - 32
            
            view = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = centerX
                y = centerY
            }
            
            // Debug positioning info
            Log.d("OverlayService", "Positioning cursor at screen center: ($centerX, $centerY)")
            
            wm.addView(cursorView, view)
            Log.d("OverlayService", "Overlay view added successfully at initial position (100, 100)")
            
            // Show toast for visibility
            Toast.makeText(this, "SubaControl overlay started", Toast.LENGTH_SHORT).show()

            // subscribe to events
            Log.d("OverlayService", "Subscribing to EventBus events")
            EventBus.events
                .onEach { e ->
                    when(e) {
                        is CursorMoveEvent -> {
                            Log.d("OverlayService", "Move event received: (${e.x}, ${e.y})")
                            view.x = e.x; view.y = e.y
                            wm.updateViewLayout(cursorView, view)
                        }
                        is TapEvent -> {
                            Log.d("OverlayService", "Tap event received at (${e.x}, ${e.y})")
                            TapAccessibilityService.instance?.simulateTap(e.x, e.y)
                        }
                    }
                }
                .launchIn(scope)

            // Initialize WebSocketReceiver with context for screen metrics and calibration
            Log.d("OverlayService", "Starting WebSocket connection (IP: ${getLocalIpAddressOrDefault()})")
            webSocketReceiver = WebSocketReceiver(this)
            webSocketReceiver?.connect()
            Toast.makeText(this, "Connecting to WebSocket server...", Toast.LENGTH_SHORT).show()
            
            Log.d("OverlayService", "OverlayService started successfully")
        } catch (e: Exception) {
            Log.e("OverlayService", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            // Stop the service if initialization fails
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("OverlayService", "onStartCommand called")
        
        // Ensure WebSocket is connected when service restarts
        if (webSocketReceiver == null) {
            Log.d("OverlayService", "Reconnecting WebSocket in onStartCommand")
            webSocketReceiver = WebSocketReceiver(this)
            webSocketReceiver?.connect()
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SubaControl Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for SubaControl overlay service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("OverlayService", "Notification channel created")
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SubaControl Active")
        .setContentText("Remote cursor control is active")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
    
    private fun getLocalIpAddressOrDefault(): String {
        // Get IP for debugging help
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE)
            val ipAddress = java.net.InetAddress.getLocalHost().hostAddress
            ipAddress ?: "192.168.18.120"
        } catch (e: Exception) {
            "192.168.18.120"
        }
    }

    override fun onDestroy() {
        try {
            webSocketReceiver?.disconnect()
            webSocketReceiver = null
            
            wm.removeView(cursorView)
            scope.cancel()
            Log.d("OverlayService", "OverlayService destroyed")
        } catch (e: Exception) {
            Log.e("OverlayService", "Error in onDestroy", e)
        }
        super.onDestroy()
    }

    override fun onBind(i: Intent?) = null
}
