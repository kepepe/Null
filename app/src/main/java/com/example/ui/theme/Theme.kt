package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF21005D),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2B8B5),
    onTertiaryContainer = Color(0xFF601410),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE9F0FF),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFE1E2EC),
    outlineVariant = Color(0xFFD1D9E6)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF70B6F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF163E66),
    onPrimaryContainer = Color(0xFFD4E7FF),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF382E54),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = Color(0xFFE5B4B2),
    onTertiary = Color(0xFF4C1D1C),
    tertiaryContainer = Color(0xFF532422),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F141C),
    onBackground = Color(0xFFE1E7F0),
    surface = Color(0xFF18202C),
    onSurface = Color(0xFFE1E7F0),
    surfaceVariant = Color(0xFF222B3A),
    onSurfaceVariant = Color(0xFF9BA8BA),
    outline = Color(0xFF263345),
    outlineVariant = Color(0xFF32425A)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val bentoPalette = if (isDark) DarkBentoPalette else LightBentoPalette
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalBentoPalette provides bentoPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
