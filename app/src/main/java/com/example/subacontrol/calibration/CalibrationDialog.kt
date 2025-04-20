package com.example.subacontrol.calibration

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import com.example.subacontrol.ui.theme.SubaColors

// Define DataStore at the app level
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calibration")

// Preference keys
object PreferenceKeys {
    val DESKTOP_MAX_X = intPreferencesKey("desktop_max_x")
    val DESKTOP_MAX_Y = intPreferencesKey("desktop_max_y")
}

// Singleton to temporarily store calibration raw coordinates
object CalibrationCoordinates {
    var topLeftRawX = 0
    var topLeftRawY = 0
    var bottomRightRawX = 1920
    var bottomRightRawY = 1080
    var isCalibrating = false
    var currentStep = 1

    fun reset() {
        topLeftRawX = 0
        topLeftRawY = 0
        bottomRightRawX = 1920
        bottomRightRawY = 1080
        isCalibrating = false
        currentStep = 1
    }
    
    fun captureTopLeft(x: Int, y: Int) {
        topLeftRawX = x
        topLeftRawY = y
        currentStep = 2
        Timber.d("Captured top-left point: ($x, $y)")
    }
    
    fun captureBottomRight(x: Int, y: Int) {
        bottomRightRawX = x
        bottomRightRawY = y
        Timber.d("Captured bottom-right point: ($x, $y)")
    }
    
    fun getDesktopWidth() = bottomRightRawX - topLeftRawX
    fun getDesktopHeight() = bottomRightRawY - topLeftRawY
}

/**
 * Calibration dialog that captures two points to determine screen mapping
 */
@Composable
fun CalibrationDialog(
    isVisible: Boolean,
    onCalibrationComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val calibrationStep = remember { mutableStateOf(CalibrationCoordinates.currentStep) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Update status text based on calibration progress
    val statusText = remember { mutableStateOf("") }
    
    // Enable calibration mode
    LaunchedEffect(Unit) {
        CalibrationCoordinates.isCalibrating = true
        Timber.d("Calibration mode enabled")
    }
    
    // Monitor for changes to calibration coordinates
    LaunchedEffect(CalibrationCoordinates.currentStep) {
        calibrationStep.value = CalibrationCoordinates.currentStep
        
        if (CalibrationCoordinates.currentStep == 2) {
            statusText.value = "Top-left point captured at (${CalibrationCoordinates.topLeftRawX}, ${CalibrationCoordinates.topLeftRawY})"
        }
    }

    Dialog(
        onDismissRequest = {
            CalibrationCoordinates.isCalibrating = false
            onDismiss()
        },
        // Make dialog non-modal to allow events to pass through to underlying views
        // This ensures cursor movement continues while calibration dialog is visible
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (calibrationStep.value == 1) 
                        "Click the TOP-LEFT target on your DESKTOP" 
                    else 
                        "Click the BOTTOM-RIGHT target on your DESKTOP",
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show calibration progress
                if (statusText.value.isNotEmpty()) {
                    Text(
                        text = statusText.value,
                        color = SubaColors.Aqua
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = if (calibrationStep.value == 1) 
                        Alignment.TopStart 
                    else 
                        Alignment.BottomEnd
                ) {
                    // Target circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SubaColors.Gold, CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (calibrationStep.value == 1)
                        "Move your cursor to the red target on your DESKTOP and click"
                    else
                        "Move your cursor to the red target on your DESKTOP and click"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Manual calibration fallback
                Text(
                    text = "If clicking doesn't work, use the button below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Add manual calibration buttons
                Button(
                    onClick = {
                        if (calibrationStep.value == 1) {
                            // Manually capture top-left at (0,0)
                            CalibrationCoordinates.captureTopLeft(0, 0)
                            Timber.d("Manual calibration: Top-left set to (0,0)")
                        } else {
                            // Manually capture bottom-right at standard desktop resolution
                            CalibrationCoordinates.captureBottomRight(1920, 1080)
                            Timber.d("Manual calibration: Bottom-right set to (1920,1080)")
                        }
                    }
                ) {
                    Text(
                        text = if (calibrationStep.value == 1) 
                            "Set Top-Left to (0,0)" 
                        else 
                            "Set Bottom-Right to (1920,1080)"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Only show Next button for second step
                if (calibrationStep.value == 2) {
                    if (CalibrationCoordinates.bottomRightRawX > CalibrationCoordinates.topLeftRawX) {
                        // Show calibration metrics
                        Text(
                            text = "Desktop size: ${CalibrationCoordinates.getDesktopWidth()} x ${CalibrationCoordinates.getDesktopHeight()} px",
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Button(
                        onClick = {
                            // Complete calibration and save
                            scope.launch {
                                // Use the actual captured values from CalibrationCoordinates
                                val desktopMaxX = CalibrationCoordinates.getDesktopWidth()
                                val desktopMaxY = CalibrationCoordinates.getDesktopHeight()
                                
                                if (desktopMaxX <= 0 || desktopMaxY <= 0) {
                                    // Use default values if calibration failed
                                    context.saveCalibrationData(1920, 1080)
                                    Timber.w("Invalid calibration values, using defaults")
                                } else {
                                    // Save valid calibration
                                    context.saveCalibrationData(desktopMaxX, desktopMaxY)
                                    Timber.d("Calibration saved: $desktopMaxX x $desktopMaxY")
                                }
                                
                                CalibrationCoordinates.isCalibrating = false
                                CalibrationCoordinates.reset()
                                onCalibrationComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "Complete Calibration")
                    }
                }
            }
        }
    }
}

/**
 * Save calibration data to DataStore
 */
suspend fun Context.saveCalibrationData(desktopMaxX: Int, desktopMaxY: Int) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.DESKTOP_MAX_X] = desktopMaxX
        preferences[PreferenceKeys.DESKTOP_MAX_Y] = desktopMaxY
    }
}

/**
 * Retrieve calibration data from DataStore
 */
fun Context.getCalibrationData(): Flow<Pair<Int, Int>> {
    return dataStore.data.map { preferences ->
        val maxX = preferences[PreferenceKeys.DESKTOP_MAX_X] ?: 1920 // Default values
        val maxY = preferences[PreferenceKeys.DESKTOP_MAX_Y] ?: 1080
        Pair(maxX, maxY)
    }
} 