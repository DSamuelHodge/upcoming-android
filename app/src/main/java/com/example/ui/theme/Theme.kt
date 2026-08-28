package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light-only theme: cream canvas + coral accent.
// The system is inherently two-tone (cream + dark content surfaces);
// a global dark-mode toggle is out of scope per upcoming-design-system.md.
private val LightColorScheme = lightColorScheme(
    primary = PrimaryCoral,
    onPrimary = OnPrimary,
    primaryContainer = SurfaceCreamStrong,
    onPrimaryContainer = Ink,
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSoft,
    onSecondaryContainer = BodyText,
    tertiary = AccentAmber,
    onTertiary = Ink,
    tertiaryContainer = SurfaceCard,
    onTertiaryContainer = Ink,
    background = CanvasCream,
    onBackground = Ink,
    surface = CanvasCream,
    onSurface = Ink,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = MutedText,
    surfaceContainer = SurfaceSoft,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceCreamStrong,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = SemanticError,
    onError = Color.White,
    errorContainer = SurfaceSoft,
    onErrorContainer = SemanticError,
)

@Composable
fun UpcomingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = UpcomingTypography,
        content = content
    )
}
