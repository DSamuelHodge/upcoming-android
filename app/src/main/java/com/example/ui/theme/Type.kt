package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Inter: primary display & body typeface
val InterFontFamily = FontFamily(
    Font(R.font.inter_300, FontWeight.Light),
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

// Instrument Serif: editorial accents (italic only)
val InstrumentSerifFontFamily = FontFamily(
    Font(R.font.instrument_serif_400_italic, FontWeight.Normal, FontStyle.Italic),
)

// DM Mono: data & precision (time, dates, metrics)
val DmMonoFontFamily = FontFamily(
    Font(R.font.dm_mono_400, FontWeight.Normal),
)

/**
 * Upcoming typography scale.
 * Display: Inter weight 400 with negative tracking — quiet, never bold.
 * Titles/Labels: Inter weight 500. Body: Inter weight 400.
 * Data: DM Mono with tabular numbers.
 */
val UpcomingTypography = Typography(
    // Hero scale (64sp) — onboarding / empty-state display
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        lineHeight = 70.4.sp,
        letterSpacing = (-1.6).sp,
    ),
    // Section heading (48sp)
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 55.2.sp,
        letterSpacing = (-1.2).sp,
    ),
    // Sub-section heading (36sp)
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 43.2.sp,
        letterSpacing = (-0.8).sp,
    ),
    // Card headline / pricing tier (28sp)
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Mobile hero fallback / sub-headings (24sp)
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.8.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Event titles, plan labels (22sp, 500)
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.6.sp,
        letterSpacing = 0.sp,
    ),
    // Feature card titles (18sp, 500)
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.2.sp,
        letterSpacing = 0.sp,
    ),
    // List item labels (16sp, 500)
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.4.sp,
        letterSpacing = 0.sp,
    ),
    // Default running text (16sp, 400)
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.8.sp,
        letterSpacing = 0.sp,
    ),
    // Secondary text (14sp, 400)
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.7.sp,
        letterSpacing = 0.sp,
    ),
    // Secondary text (14sp, 400) — body-sm token (Inter; mono data styles live in UpcomingTextStyles)
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.7.sp,
        letterSpacing = 0.sp,
    ),
    // Button labels (14sp, 500)
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
    // Badge labels, captions (12sp, 500)
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.sp,
    ),
    // UPPERCASE badges: NEW, priority tags (12sp, 500, +1.5sp tracking)
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 1.5.sp,
    ),
)

/** Custom styles outside the Material3 slot system. */
object UpcomingTextStyles {
    // Editorial asides, emphasized quotes (16sp Instrument Serif italic)
    val serifItalic = TextStyle(
        fontFamily = InstrumentSerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 24.8.sp,
        letterSpacing = 0.sp,
    )

    // Time labels HH:MM, date displays (12sp DM Mono, tabular)
    val monoLabel = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    // API endpoints, config values (14sp DM Mono, tabular)
    val monoData = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    // Top nav menu items (14sp Inter 500)
    val navLink = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.6.sp,
        letterSpacing = 0.sp,
    )

    // Badge labels, metadata (13sp Inter 500)
    val caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.2.sp,
        letterSpacing = 0.sp,
    )
}
