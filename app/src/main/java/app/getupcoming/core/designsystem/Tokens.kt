package app.getupcoming.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.getupcoming.ui.theme.AccentAmber
import app.getupcoming.ui.theme.AccentTeal
import app.getupcoming.ui.theme.BodyText
import app.getupcoming.ui.theme.CanvasCream
import app.getupcoming.ui.theme.Hairline
import app.getupcoming.ui.theme.HairlineSoft
import app.getupcoming.ui.theme.Ink
import app.getupcoming.ui.theme.MutedSoftText
import app.getupcoming.ui.theme.MutedText
import app.getupcoming.ui.theme.PrimaryCoral
import app.getupcoming.ui.theme.PrimaryCoralActive
import app.getupcoming.ui.theme.PrimaryCoralDisabled
import app.getupcoming.ui.theme.SemanticError
import app.getupcoming.ui.theme.SemanticSuccess
import app.getupcoming.ui.theme.SurfaceCard
import app.getupcoming.ui.theme.SurfaceCreamStrong
import app.getupcoming.ui.theme.SurfaceDark
import app.getupcoming.ui.theme.SurfaceDarkElevated
import app.getupcoming.ui.theme.SurfaceSoft

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
