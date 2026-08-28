package com.example.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BodyText
import com.example.ui.theme.CanvasCream
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineSoft
import com.example.ui.theme.Ink
import com.example.ui.theme.MutedSoftText
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralActive
import com.example.ui.theme.PrimaryCoralDisabled
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCreamStrong
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.SurfaceSoft

object UpcomingTokens {
    // Spacing (4dp base)
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingXLarge = 32.dp
    val SpacingXXLarge = 48.dp
    val SpacingSection = 64.dp

    // Corner radius scale (rounded.md 8 = buttons/inputs, lg 12 = cards, xl 16 = hero)
    val RadiusXSmall = RoundedCornerShape(4.dp)
    val RadiusSmall = RoundedCornerShape(6.dp)
    val RadiusMedium = RoundedCornerShape(8.dp)
    val RadiusLarge = RoundedCornerShape(12.dp)
    val RadiusXLarge = RoundedCornerShape(16.dp)
    val RadiusFull = RoundedCornerShape(100.dp)

    // Elevation / borders: color-block first, shadow rare
    val BorderWidth = 1.dp
    val CardElevation = 0.dp

    // Surfaces
    val CanvasBg = CanvasCream
    val SurfaceSoftBg = SurfaceSoft
    val CardBg = SurfaceCard
    val CreamStrongBg = SurfaceCreamStrong
    val DarkSurface = SurfaceDark
    val DarkSurfaceElevated = SurfaceDarkElevated

    // Borders
    val BorderDefault = Hairline
    val BorderSubtle = HairlineSoft

    // Brand accent (coral — scarce: primary CTAs and key interactions only)
    val BrandPrimary = PrimaryCoral
    val BrandPrimaryActive = PrimaryCoralActive
    val BrandPrimaryDisabled = PrimaryCoralDisabled

    // Text tones
    val TextPrimary = Ink
    val TextBody = BodyText
    val TextMuted = MutedText
    val TextMutedSoft = MutedSoftText

    // Selection / highlight states
    val SelectedBg = SurfaceCreamStrong
    val SelectedBorder = PrimaryCoral

    // Location-type accents (companion palette)
    val DailyVideoAccent = AccentTeal
    val GoogleMeetAccent = SemanticSuccess
    val PhoneAccent = AccentAmber
    val InPersonAccent = PrimaryCoral
    val VirtualAccent = MutedText

    // Status accents
    val SuccessAccent = SemanticSuccess
    val ErrorAccent = SemanticError
}
