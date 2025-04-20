package com.example.subacontrol

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.subacontrol.accessibility.TapAccessibilityService
import com.example.subacontrol.calibration.CalibrationDialog
import com.example.subacontrol.calibration.getCalibrationData
import com.example.subacontrol.debug.TestServer
import com.example.subacontrol.overlay.CursorOverlay
import com.example.subacontrol.overlay.OverlayService
import com.example.subacontrol.ui.ControlPanelScreen
import com.example.subacontrol.util.WebSocketManager
import com.example.subacontrol.ui.theme.SubaControlTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("MainActivity", "Overlay permission result received")
        if (Settings.canDrawOverlays(this)) {
            Log.d("MainActivity", "Overlay permission granted")
            checkCalibrationAndStart()
        } else {
            Log.e("MainActivity", "Overlay permission denied")
        }
    }

    private var testServer: TestServer? = null
    private var isFirstRun = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize logger
        Log.d("MainActivity", "App starting")
        
        // Set up Compose UI
        setContent {
            SubaControlTheme {
                ControlPanelScreen(
                    onCalibrationComplete = {
                        // Restart services after calibration
                        if (Settings.canDrawOverlays(this) && 
                            TapAccessibilityService.instance != null) {
                            startCursorOverlay(this)
                            WebSocketManager.connect(this)
                        }
                    }
                )
            }
        }
    }
    
    private fun checkCalibrationAndStart() {
        lifecycleScope.launch {
            val calibrationData = getCalibrationData().first()
            Log.d("MainActivity", "Calibration data: ${calibrationData.first}x${calibrationData.second}")
            
            if (calibrationData.first == 1920 && calibrationData.second == 1080) {
                // Default values, show calibration dialog
                Log.d("MainActivity", "Using default calibration, showing dialog")
                setContent {
                    SubaControlTheme {
                        var showCalibration by remember { mutableStateOf(true) }
                        
                        CalibrationDialog(
                            isVisible = showCalibration,
                            onCalibrationComplete = {
                                showCalibration = false
                                startOverlayService()
                            },
                            onDismiss = {
                                // Use default values
                                showCalibration = false
                                startOverlayService()
                            }
                        )
                    }
                }
            } else {
                // Calibration data exists, start service directly
                Log.d("MainActivity", "Using existing calibration data")
                startOverlayService()
            }
        }
    }
    
    private fun startOverlayService() {
        Log.d("MainActivity", "Starting OverlayService")
        // Check if the service is already running
        if (isServiceRunning(OverlayService::class.java)) {
            Log.d("MainActivity", "OverlayService is already running")
        } else {
            try {
                val serviceIntent = Intent(this, OverlayService::class.java)
                
                // Flag to ensure service stays running
                serviceIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Log.d("MainActivity", "OverlayService started successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting OverlayService", e)
            }
        }
        
        // Comment out the test server start
        // testServer?.start()
        
        // Don't finish the activity immediately - let it stay in memory briefly
        // This helps prevent the service from being killed immediately
        if (isFirstRun) {
            isFirstRun = false
            Log.d("MainActivity", "First run - keeping MainActivity open briefly")
            
            // Wait a moment before finishing to ensure service starts properly
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                Log.d("MainActivity", "Finishing MainActivity after delay")
                finish()
            }
        } else {
            Log.d("MainActivity", "Finishing MainActivity immediately")
            finish()
        }
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    override fun onResume() {
        super.onResume()
        
        // Check permissions on resume
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        // Check for overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Log.d("MainActivity", "Overlay permission not granted")
            // UI will handle opening settings
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        testServer?.stop()
        
        // Don't disconnect WebSocket when activity is destroyed
        // Let the service handle the connection lifecycle
    }
    
    companion object {
        // Helper function to start the cursor overlay service
        fun startCursorOverlay(context: Context) {
            val serviceIntent = Intent(context, CursorOverlay::class.java).apply {
                // Add flags to ensure the service starts even if it was stopped
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                action = "RESTART_CURSOR_OVERLAY" // Custom action to force restart
            }
            
            try {
                // Stop any existing service first to ensure a clean restart
                context.stopService(Intent(context, CursorOverlay::class.java))
                
                // Small delay to ensure service has time to stop
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d("MainActivity", "CursorOverlay service restarted")
                }, 200)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error restarting cursor overlay: ${e.message}", e)
            }
        }
    }
}

