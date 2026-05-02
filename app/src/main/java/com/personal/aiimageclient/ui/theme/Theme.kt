package com.personal.aiimageclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B7A75),
    secondary = Color(0xFF5F6C2F),
    tertiary = Color(0xFF8A4F2C),
    background = Color(0xFFF8FAF8),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EDBD2),
    secondary = Color(0xFFD1DD87),
    tertiary = Color(0xFFFFB68F),
    background = Color(0xFF101413),
    surface = Color(0xFF171C1B)
)

@Composable
fun AIImageClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

