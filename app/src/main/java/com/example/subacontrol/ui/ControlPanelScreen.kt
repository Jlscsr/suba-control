package com.example.subacontrol.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subacontrol.accessibility.TapAccessibilityService
import com.example.subacontrol.calibration.CalibrationDialog
import com.example.subacontrol.overlay.CursorOverlay
import com.example.subacontrol.util.ConnectionState
import com.example.subacontrol.util.WebSocketManager
import kotlinx.coroutines.delay
import com.example.subacontrol.ui.theme.SubaColors

/**
 * Main control panel screen showing connection status and permission controls
 */
@Composable
fun ControlPanelScreen(
    onCalibrationComplete: () -> Unit
) {
    val context = LocalContext.current
    
    // Track UI states
    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityEnabled by remember { 
        mutableStateOf(TapAccessibilityService.instance != null) 
    }
    
    var showCalibrationDialog by remember { mutableStateOf(false) }
    
    // Get WebSocket connection state
    val connectionState by WebSocketManager.connectionState.collectAsState()
    
    // Control UI appearance
    val isLoading = connectionState == ConnectionState.CONNECTING
    val isConnected = connectionState == ConnectionState.CONNECTED
    
    // Check permission states periodically
    LaunchedEffect(Unit) {
        while (true) {
            overlayPermissionGranted = Settings.canDrawOverlays(context)
            accessibilityEnabled = TapAccessibilityService.instance != null
            
            // Start services if permissions granted
            if (overlayPermissionGranted && accessibilityEnabled && 
                connectionState == ConnectionState.DISCONNECTED) {
                WebSocketManager.connect(context)
                startCursorOverlay(context)
            }
            
            delay(1000)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SubaControl",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Required Permissions",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Overlay permission toggle
                    PermissionToggle(
                        title = "Display Overlay",
                        subtitle = "Allow drawing cursor over other apps",
                        emoji = "👁️",
                        isGranted = overlayPermissionGranted,
                        onClick = {
                            // Open overlay permission settings
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Accessibility service toggle
                    PermissionToggle(
                        title = "Accessibility Service",
                        subtitle = "Allow simulating taps on screen",
                        emoji = "👆",
                        isGranted = accessibilityEnabled,
                        onClick = {
                            // Open accessibility settings
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Connection status and loader
            ConnectionStatus(
                connectionState = connectionState,
                isLoading = isLoading
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Calibration button
            Button(
                onClick = { showCalibrationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calibrate Screen")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restart Cursor button
            OutlinedButton(
                onClick = {
                    startCursorOverlay(context)
                    Toast.makeText(context, "Restarting cursor overlay...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Restart Cursor")
            }

            // Debug info button
            OutlinedButton(
                onClick = {
                    // Display screen dimensions and scaling info
                    val metrics = context.resources.displayMetrics
                    val phoneWidth = metrics.widthPixels
                    val phoneHeight = metrics.heightPixels
                    
                    // Show detailed info using toast
                    Toast.makeText(
                        context,
                        "Screen: ${phoneWidth}x${phoneHeight}px\n" +
                        "Mapping: Full screen mapping active",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Log additional information
                    Log.d("DebugInfo", "Screen dimensions: ${phoneWidth}x${phoneHeight}px")
                    Log.d("DebugInfo", "Screen density: ${metrics.density} (DPI: ${metrics.densityDpi})")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Debug Info",
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Display Debug Info", color = MaterialTheme.colorScheme.tertiary)
            }
        }
        
        // Show calibration dialog if requested
        if (showCalibrationDialog) {
            CalibrationDialog(
                isVisible = true,
                onCalibrationComplete = {
                    showCalibrationDialog = false
                    onCalibrationComplete()
                },
                onDismiss = {
                    showCalibrationDialog = false
                }
            )
        }
    }
}

@Composable
fun PermissionToggle(
    title: String,
    subtitle: String,
    emoji: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Switch(
            checked = isGranted,
            onCheckedChange = { if (!isGranted) onClick() }
        )
    }
}

@Composable
fun ConnectionStatus(
    connectionState: ConnectionState,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Text(
            when (connectionState) {
                ConnectionState.DISCONNECTED -> "Waiting for permissions..."
                ConnectionState.CONNECTING -> "Connecting to server..."
                ConnectionState.CONNECTED -> "Connected! Cursor is active"
                ConnectionState.ERROR -> "Connection error. Check server"
            },
            modifier = Modifier.padding(start = if (isLoading) 12.dp else 0.dp),
            color = when (connectionState) {
                ConnectionState.CONNECTED -> SubaColors.Aqua
                ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// Helper function to start the cursor overlay service
private fun startCursorOverlay(context: Context) {
    val serviceIntent = Intent(context, CursorOverlay::class.java).apply {
        // Add flags to ensure the service starts even if it was stopped
        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        action = "RESTART_CURSOR_OVERLAY" // Custom action to force restart
    }
    
    try {
        // Stop any existing service first to ensure a clean restart
        context.stopService(Intent(context, CursorOverlay::class.java))
        
        // Small delay to ensure service has time to stop
        Handler(Looper.getMainLooper()).postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("ControlPanel", "CursorOverlay service restarted")
        }, 200)
    } catch (e: Exception) {
        Log.e("ControlPanel", "Error restarting cursor overlay: ${e.message}", e)
    }
} 