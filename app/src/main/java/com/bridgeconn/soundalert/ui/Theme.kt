package com.bridgeconn.soundalert.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D77),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3F1),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF455A64),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CDAD8),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F52),
    onPrimaryContainer = Color(0xFFA0F0EE),
    secondary = Color(0xFFB8C8CE),
    background = Color(0xFF101416),
    surface = Color(0xFF171C1E),
    error = Color(0xFFFFB4AB)
)

@Composable
fun SoundAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
