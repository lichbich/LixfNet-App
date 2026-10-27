package com.lixfnet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NetflixDarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    primaryContainer = NetflixDarkRed,
    onPrimaryContainer = Color.White,
    secondary = NetflixDarkCard,
    onSecondary = Color.White,
    background = NetflixBackground,
    onBackground = NetflixTextPrimary,
    surface = NetflixDarkCard,
    onSurface = NetflixTextPrimary,
    surfaceVariant = NetflixCardHover,
    onSurfaceVariant = NetflixTextSecondary,
    outline = NetflixBorder
)

@Composable
fun LixfNetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NetflixDarkColorScheme,
        typography = Typography,
        content = content
    )
}
