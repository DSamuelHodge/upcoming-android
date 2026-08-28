package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GeoPrimaryDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = GeoPrimaryContainerDark,
    onPrimaryContainer = GeoOnPrimaryContainerDark,
    secondary = GeoPurpleLight,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = GeoSurfaceContainerDark,
    onSecondaryContainer = GeoTextPrimaryDark,
    tertiary = GeoGreenDot,
    onTertiary = Color.Black,
    background = GeoCanvasDark,
    onBackground = GeoTextPrimaryDark,
    surface = GeoSurfaceDark,
    onSurface = GeoTextPrimaryDark,
    surfaceVariant = GeoSurfaceContainerDark,
    onSurfaceVariant = GeoTextSecondaryDark,
    outline = GeoBorderDark,
    outlineVariant = GeoBorderSubtleDark,
    error = GeoRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPrimaryContainerLight,
    onPrimaryContainer = GeoOnPrimaryContainerLight,
    secondary = GeoPurple,
    onSecondary = Color.White,
    secondaryContainer = GeoSurfaceContainerLight,
    onSecondaryContainer = GeoTextPrimaryLight,
    tertiary = GeoGreen,
    onTertiary = Color.White,
    background = GeoCanvasLight,
    onBackground = GeoTextPrimaryLight,
    surface = GeoSurfaceLight,
    onSurface = GeoTextPrimaryLight,
    surfaceVariant = GeoSurfaceContainerLight,
    onSurfaceVariant = GeoTextSecondaryLight,
    outline = GeoBorderLight,
    outlineVariant = GeoBorderSubtleLight,
    error = GeoRose,
    onError = Color.White
)

@Composable
fun UpcomingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Geometric Balance aesthetic consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = UpcomingTheme(darkTheme, dynamicColor, content)

