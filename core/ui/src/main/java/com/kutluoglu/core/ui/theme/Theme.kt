package com.kutluoglu.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Define the Dark Color Scheme using your custom colors
private val DarkColorScheme = darkColorScheme(
    primary = IslamicGold,              // Your new primary color
    onPrimary = IslamicDarkBg,          // Color for text/icons on top of primary
    primaryContainer = IslamicGold,     // A container using the primary color
    onPrimaryContainer = IslamicDarkBg,
    secondary = IslamicCardBg,
    onSecondary = IslamicWhite,
    background = IslamicDarkBg,
    onBackground = IslamicWhite,
    surface = IslamicCardBg,
    onSurface = IslamicWhite,
    onSurfaceVariant = IslamicTextSecondary,
    error = Color.Red,
    onError = Color.White
)

// Define the Light Color Scheme using your custom colors
private val LightColorScheme = lightColorScheme(
    primary = IslamicGold,              // Your new primary color
    onPrimary = IslamicDarkBg,          // Use a dark color for text on the gold background
    primaryContainer = IslamicGold,
    onPrimaryContainer = IslamicDarkBg,
    secondary = IslamicWhite,
    onSecondary = IslamicDarkBg,
    background = IslamicSurface,
    onBackground = IslamicDarkBg,
    surface = IslamicWhite,
    onSurface = IslamicDarkBg,
    onSurfaceVariant = IslamicTextSecondary,
    error = Color.Red,
    onError = Color.White

    /* Other default colors to override, if needed */
)

@Composable
fun NamazVakitleriTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false, content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // THIS IS THE KEY CHANGE:
            // Hide the status bar completely.
            insetsController.hide(WindowInsetsCompat.Type.statusBars())

            // Define the behavior for when the user swipes from the edge.
            // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE means the bar will appear
            // temporarily and then hide again.
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        /*SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }*/
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}

