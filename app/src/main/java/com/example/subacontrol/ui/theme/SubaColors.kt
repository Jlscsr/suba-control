package com.example.subacontrol.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Oozora Subaru color palette for SubaControl
 */
object SubaColors {
    // Primary colors - Navy blue from Subaru's jacket
    val Navy = Color(0xFF1F3057)        // Primary - Subaru's navy jacket
    val NavyLight = Color(0xFF29447A)   // PrimaryContainer - Lighter navy for containers
    
    // Secondary colors - Gold from Subaru's buttons and star
    val Gold = Color(0xFFF5D96B)        // Secondary - Gold buttons and star
    val GoldLight = Color(0xFFFFF3C1)   // SecondaryContainer - Pale gold for containers
    
    // Accent color - Aqua from Subaru's ribbon
    val Aqua = Color(0xFF3BC3F3)        // Tertiary - Aqua ribbon accent
    
    // Error color
    val Error = Color(0xFFD03B3B)       // Error red
    
    // Background and surfaces
    val White = Color(0xFFFFFFFF)       // Background/surface - Screen white
    
    // Text colors based on contrast requirements
    val OnNavy = Color(0xFFFFFFFF)      // White text on navy (high contrast)
    val OnGold = Color(0xFF1F3057)      // Navy text on gold (high contrast)
    
    // Semi-transparent navy for cursor (80% opacity)
    val NavyTransparent = Color(0xCC1F3057)  // Semi-transparent navy for cursor
} 