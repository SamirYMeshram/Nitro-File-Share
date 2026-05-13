package com.nitrodropnative.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = NitroCyan,
    secondary = NitroBlue,
    tertiary = NitroGreen,
    background = NitroBackground,
    surface = NitroSurface,
    onPrimary = NitroBackground,
    onSecondary = NitroBackground,
    onBackground = NitroText,
    onSurface = NitroText,
    error = NitroDanger
)

@Composable
fun NitroDropTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NitroBackground.toArgb()
            window.navigationBarColor = NitroBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = NitroTypography,
        content = content
    )
}
