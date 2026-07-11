package com.formuladock.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.formuladock.core.preferences.FormulaDockPreferences
import com.formuladock.core.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78A9FF),
    onPrimary = Color(0xFF002D9C),
    primaryContainer = Color(0xFF0043CE),
    onPrimaryContainer = Color(0xFFD0E1FF),
    secondary = Color(0xFF6FEBFF),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F4FF),
    tertiary = Color(0xFFFFB0C8),
    onTertiary = Color(0xFF5C1130),
    tertiaryContainer = Color(0xFF7D2947),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF12161A),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF1A1F24),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF40474F),
    onSurfaceVariant = Color(0xFFC0C7D0),
    outline = Color(0xFF8A929A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F62FE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E1FF),
    onPrimaryContainer = Color(0xFF001D6F),
    secondary = Color(0xFF006874),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF97F4FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF9B405E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1F24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1F24),
    surfaceVariant = Color(0xFFE0E2E5),
    onSurfaceVariant = Color(0xFF40474F),
    outline = Color(0xFF70777F)
)

@Composable
fun FormulaDockTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val preferences = remember { FormulaDockPreferences() }
    val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.System)
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = darkTheme ?: when (themeMode) {
        ThemeMode.System -> systemInDarkTheme
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
