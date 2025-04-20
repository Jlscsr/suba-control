package com.example.subacontrol.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme for Oozora Subaru themed app
 */
private val LightColorScheme = lightColorScheme(
    primary = SubaColors.Navy,
    onPrimary = SubaColors.OnNavy,
    primaryContainer = SubaColors.NavyLight,
    onPrimaryContainer = SubaColors.OnNavy,
    
    secondary = SubaColors.Gold,
    onSecondary = SubaColors.OnGold,
    secondaryContainer = SubaColors.GoldLight,
    onSecondaryContainer = SubaColors.OnGold,
    
    tertiary = SubaColors.Aqua,
    onTertiary = SubaColors.OnNavy,
    
    error = SubaColors.Error,
    onError = SubaColors.OnNavy,
    
    background = SubaColors.White,
    onBackground = SubaColors.Navy,
    
    surface = SubaColors.White,
    onSurface = SubaColors.Navy
)

/**
 * Dark color scheme stub for future implementation
 * Currently uses same colors as light scheme with slight adjustments
 */
private val DarkColorScheme = darkColorScheme(
    // Use same colors for now - future implementation can adjust these
    primary = SubaColors.Navy,
    onPrimary = SubaColors.OnNavy,
    primaryContainer = SubaColors.NavyLight,
    onPrimaryContainer = SubaColors.OnNavy,
    
    secondary = SubaColors.Gold,
    onSecondary = SubaColors.OnGold,
    secondaryContainer = SubaColors.GoldLight,
    onSecondaryContainer = SubaColors.OnGold,
    
    tertiary = SubaColors.Aqua,
    onTertiary = SubaColors.OnNavy,
    
    error = SubaColors.Error,
    onError = SubaColors.OnNavy,
    
    background = SubaColors.White,
    onBackground = SubaColors.Navy,
    
    surface = SubaColors.White,
    onSurface = SubaColors.Navy
)

/**
 * SubaControlTheme: Applies Oozora Subaru's navy-and-gold palette to the app
 * using Material 3 color system for consistency.
 */
@Composable
fun SubaControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is set to false to ensure consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // We currently use light scheme for both light and dark modes
        // Future implementation can use proper dark theme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Set status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Use light text for dark status bar background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
} 