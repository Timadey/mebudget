package com.mebudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Shell,
    primaryContainer = Sand,
    onPrimaryContainer = Charcoal,
    secondary = Moss,
    onSecondary = Shell,
    tertiary = Info,
    onTertiary = Shell,
    background = Canvas,
    onBackground = Charcoal,
    surface = Shell,
    onSurface = Charcoal,
    surfaceVariant = Sand,
    onSurfaceVariant = Fog,
    error = Overspend,
    errorContainer = Color(0xFFF5DDD7),
    onError = Color.White,
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = Charcoal,
    primaryContainer = Pine,
    onPrimaryContainer = Shell,
    secondary = Moss,
    onSecondary = Shell,
    tertiary = Color(0xFF8EB6B2),
    onTertiary = Charcoal,
    background = NightCanvas,
    onBackground = Shell,
    surface = NightSurface,
    onSurface = Shell,
    surfaceVariant = NightSand,
    onSurfaceVariant = Color(0xFFD4C8BB),
    error = Color(0xFFFFB7A7),
    errorContainer = Color(0xFF5A241B),
    onError = Color(0xFF44170F),
    onErrorContainer = Color(0xFFFFDBD3)
)

@Composable
fun MeBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

@Composable
fun BrutalistBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrutalistLightColors,
        typography = Typography,
        content = content
    )
}
