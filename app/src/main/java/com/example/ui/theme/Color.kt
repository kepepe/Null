package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class BentoPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val borderLight: Color,
    val borderContainer: Color,
    val borderAccent: Color,
    val purpleContainer: Color,
    val onPurpleContainer: Color,
    val coralContainer: Color,
    val onCoralContainer: Color,
    val greenContainer: Color,
    val successGreen: Color,
    val isDark: Boolean
)

val LightBentoPalette = BentoPalette(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE9F0FF),
    onSurfaceVariant = Color(0xFF44474E),
    borderLight = Color(0xFFE1E2EC),
    borderContainer = Color(0xFFBAC7DB),
    borderAccent = Color(0xFFD1D9E6),
    purpleContainer = Color(0xFFEADDFF),
    onPurpleContainer = Color(0xFF21005D),
    coralContainer = Color(0xFFF2B8B5),
    onCoralContainer = Color(0xFF601410),
    greenContainer = Color(0xFFD4F7DF),
    successGreen = Color(0xFF008844),
    isDark = false
)

val DarkBentoPalette = BentoPalette(
    primary = Color(0xFF70B6F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF163E66),
    onPrimaryContainer = Color(0xFFD4E7FF),
    background = Color(0xFF0F141C),
    onBackground = Color(0xFFE1E7F0),
    surface = Color(0xFF18202C),
    onSurface = Color(0xFFE1E7F0),
    surfaceVariant = Color(0xFF222B3A),
    onSurfaceVariant = Color(0xFF9BA8BA),
    borderLight = Color(0xFF263345),
    borderContainer = Color(0xFF32425A),
    borderAccent = Color(0xFF3B4D68),
    purpleContainer = Color(0xFF382E54),
    onPurpleContainer = Color(0xFFE9DDFF),
    coralContainer = Color(0xFF532422),
    onCoralContainer = Color(0xFFFFDAD6),
    greenContainer = Color(0xFF133824),
    successGreen = Color(0xFF4EE28E),
    isDark = true
)

val LocalBentoPalette = compositionLocalOf { LightBentoPalette }

// Composable accessors so existing references reactively switch on theme toggle
val BentoPrimary: Color @Composable get() = LocalBentoPalette.current.primary
val BentoOnPrimary: Color @Composable get() = LocalBentoPalette.current.onPrimary
val BentoPrimaryContainer: Color @Composable get() = LocalBentoPalette.current.primaryContainer
val BentoOnPrimaryContainer: Color @Composable get() = LocalBentoPalette.current.onPrimaryContainer

val BentoBackground: Color @Composable get() = LocalBentoPalette.current.background
val BentoOnBackground: Color @Composable get() = LocalBentoPalette.current.onBackground

val BentoSurface: Color @Composable get() = LocalBentoPalette.current.surface
val BentoOnSurface: Color @Composable get() = LocalBentoPalette.current.onSurface

val BentoSurfaceVariant: Color @Composable get() = LocalBentoPalette.current.surfaceVariant
val BentoOnSurfaceVariant: Color @Composable get() = LocalBentoPalette.current.onSurfaceVariant

val BentoBorderLight: Color @Composable get() = LocalBentoPalette.current.borderLight
val BentoBorderContainer: Color @Composable get() = LocalBentoPalette.current.borderContainer
val BentoBorderAccent: Color @Composable get() = LocalBentoPalette.current.borderAccent

val BentoPurpleContainer: Color @Composable get() = LocalBentoPalette.current.purpleContainer
val BentoOnPurpleContainer: Color @Composable get() = LocalBentoPalette.current.onPurpleContainer

val BentoCoralContainer: Color @Composable get() = LocalBentoPalette.current.coralContainer
val BentoOnCoralContainer: Color @Composable get() = LocalBentoPalette.current.onCoralContainer

val BentoSuccessGreen: Color @Composable get() = LocalBentoPalette.current.successGreen
val BentoGreenContainer: Color @Composable get() = LocalBentoPalette.current.greenContainer
