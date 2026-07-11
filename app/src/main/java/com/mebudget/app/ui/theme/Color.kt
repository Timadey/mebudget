package com.mebudget.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Legacy earthy palette (kept for gradient compatibility)
val Canvas = Color(0xFFF6F1E8)
val Shell = Color(0xFFFFFBF5)
val Sand = Color(0xFFE8DDCE)
val Bark = Color(0xFFB79F86)
val Pine = Color(0xFF1F4D43)
val Moss = Color(0xFF5E7B62)
val Lagoon = Color(0xFF3C6E71)
val Rust = Color(0xFF9E4E3D)
val Charcoal = Color(0xFF1C1A17)
val Fog = Color(0xFF6C665D)
val NightCanvas = Color(0xFF171614)
val NightSurface = Color(0xFF22201D)
val NightSand = Color(0xFF3B3731)

// Neo-brutalist accent colors
val AccentBlue = Color(0xFF0055FF)
val AccentBlueDark = Color(0xFF3388FF)
val MutedLight = Color(0xFF666666)
val MutedDark = Color(0xFF999999)

// Semantic status colors
val Success = Color(0xFF00CC66)
val SuccessDark = Color(0xFF00FF88)
val Info = Lagoon
val Warning = Color(0xFFFFB800)
val WarningDark = Color(0xFFFFD633)
val Overspend = Color(0xFFFF0033)
val OverspendDark = Color(0xFFFF4466)

val BrutalistLightColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = MutedLight,
    error = Overspend,
    errorContainer = Color(0xFFFFF0F0),
    onError = Color.White,
    onErrorContainer = Color(0xFFCC0000),
    outline = Color.Black,
    outlineVariant = Color.Black
)

val BrutalistDarkColors = darkColorScheme(
    primary = AccentBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = MutedDark,
    error = OverspendDark,
    errorContainer = Color(0xFF442222),
    onError = Color.White,
    onErrorContainer = Color(0xFFFFAAAA),
    outline = Color.White,
    outlineVariant = Color.White
)
