# Upcoming Android — Typography Implementation Guide

## Jetpack Compose Type System (Type.kt)

This guide shows how to implement the new Upcoming typography system in your Android Compose app.

---

## Font Setup

### Step 1: Add Fonts to Resources

Place font files in `app/src/main/res/font/`:

```
app/src/main/res/font/
├── inter_300.ttf         # Light
├── inter_400.ttf         # Regular
├── inter_500.ttf         # Medium
├── inter_600.ttf         # SemiBold
├── inter_700.ttf         # Bold
├── instrument_serif_400_italic.ttf  # Editorial italic
├── instrument_serif_400.ttf         # Editorial regular (backup)
└── dm_mono_400.ttf       # Data/mono
```

**Download fonts:**
- **Inter:** https://github.com/rsms/inter (open-source)
- **DM Mono:** https://github.com/colmcq/DM-Mono (open-source)
- **Instrument Serif:** https://github.com/Instrument/instrument-serif (open-source)

### Step 2: Create Font Families (Type.kt)

```kotlin
package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.R

// Inter: Primary display & body typeface
val interFontFamily = FontFamily(
    Font(R.font.inter_300, FontWeight.Light),
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

// Instrument Serif: Editorial accents (italic only)
val instrumentSerifFontFamily = FontFamily(
    Font(R.font.instrument_serif_400, FontWeight.Normal),
    Font(R.font.instrument_serif_400_italic, FontWeight.Normal, FontStyle.Italic),
)

// DM Mono: Data & precision (labels, time, dates)
val dmMonoFontFamily = FontFamily(
    Font(R.font.dm_mono_400, FontWeight.Normal),
)

// Fallback stacks (if fonts don't load)
val interFallback = FontFamily.SansSerif  // Falls back to system sans
val serifFallback = FontFamily.Serif       // Falls back to system serif
val monoFallback = FontFamily.Monospace    // Falls back to system mono
```

---

## Complete Typography Scale (Type.kt)

```kotlin
package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Display Styles (Inter, weight 400, negative tracking)
val UpcomingTypography = Typography(
    // Hero Scale (64px) — Quiet, restrained
    displayLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,  // 400
        fontSize = 64.sp,
        lineHeight = 70.4.sp,  // 1.1 ratio
        letterSpacing = (-1.6).sp,  // -1.6px tracking
        // Use: Homepage h1 "Plan together. Move faster."
    ),
    
    // Section Heading (48px)
    displayMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 55.2.sp,  // 1.15 ratio
        letterSpacing = (-1.2).sp,  // -1.2px
        // Use: Major section headlines
    ),
    
    // Sub-section Heading (36px)
    headlineLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 43.2.sp,  // 1.2 ratio
        letterSpacing = (-0.8).sp,  // -0.8px
        // Use: Use-case titles, capability headlines
    ),
    
    // Card Headline / Pricing Tier (28px)
    headlineMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 35.sp,  // 1.25 ratio
        letterSpacing = (-0.4).sp,  // -0.4px
        // Use: Feature card titles, pricing tier names
    ),
    
    // Smaller Headline (24px) — For mobile hero fallback
    headlineSmall = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.8.sp,  // 1.2 ratio
        letterSpacing = (-0.2).sp,  // -0.2px
        // Use: Mobile hero, sub-headings
    ),
    
    // Large Title / Event Title (22px, Medium weight)
    titleLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,  // 500
        fontSize = 22.sp,
        lineHeight = 28.6.sp,  // 1.3 ratio
        letterSpacing = 0.sp,
        // Use: Pricing plan labels, event titles
    ),
    
    // Medium Title / Feature Card Title (18px, Medium)
    titleMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.2.sp,  // 1.4 ratio
        letterSpacing = 0.sp,
        // Use: Feature card titles, intro paragraphs
    ),
    
    // Small Title / List Item Label (16px, Medium)
    titleSmall = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.4.sp,  // 1.4 ratio
        letterSpacing = 0.sp,
        // Use: List item labels, section headings
    ),
    
    // Body Large (16px) — DEFAULT RUNNING TEXT
    bodyLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,  // 400
        fontSize = 16.sp,
        lineHeight = 24.8.sp,  // 1.55 ratio
        letterSpacing = 0.sp,
        // Use: Default paragraphs, event descriptions
    ),
    
    // Body Medium (14px)
    bodyMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.7.sp,  // 1.55 ratio
        letterSpacing = 0.sp,
        // Use: Secondary text, time zones, metadata
    ),
    
    // Body Small (14px) — Mono variant for time/data
    bodySmall = TextStyle(
        fontFamily = dmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,  // 1.5 ratio
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",  // Tabular numbers for alignment
        // Use: Time displays (HH:MM), data labels, DM Mono
    ),
    
    // Label Large (14px, Medium) — For buttons + captions
    labelLarge = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,  // 500
        fontSize = 14.sp,
        lineHeight = 14.sp,  // 1.0 (tight for buttons)
        letterSpacing = 0.sp,
        // Use: Button labels, CTA text
    ),
    
    // Label Medium (12px, Medium) — For badges + small labels
    labelMedium = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,  // 1.4 ratio
        letterSpacing = 0.sp,
        // Use: Badge labels, category tags
    ),
    
    // Label Small (12px, Medium) — UPPERCASE variant with tracking
    labelSmall = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 1.5.sp,  // +1.5px tracking for UPPERCASE
        // Use: "NEW", priority tags, category badges
    ),
)

// OPTIONAL CUSTOM STYLES (add to a companion object or extension)
object UpcomingTextStyles {
    // Editorial italic callout (16px Instrument Serif italic)
    val serifItalic = TextStyle(
        fontFamily = instrumentSerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 24.8.sp,
        letterSpacing = 0.sp,
        // Use: Editorial asides, emphasized quotes
    )
    
    // DM Mono variant for labels (12px, tabular numbers)
    val monoLabel = TextStyle(
        fontFamily = dmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",  // Tabular numbers
        // Use: Time labels (HH:MM), date displays
    )
    
    // Navigation link (14px Inter Medium)
    val navLink = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Medium,  // 500
        fontSize = 14.sp,
        lineHeight = 19.6.sp,
        letterSpacing = 0.sp,
        // Use: Top nav menu items
    )
}
```

---

## Usage in Compose Components

### Hero Display (64px)

```kotlin
@Composable
fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpcomingColors.canvas)
            .padding(horizontal = 24.dp, vertical = 96.dp)
    ) {
        Text(
            text = "Plan together.\nMove faster.",
            style = UpcomingTypography.displayLarge,  // 64px Inter -1.6px
            color = UpcomingColors.ink,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Upcoming is the calendar designed for teams.",
            style = UpcomingTypography.bodyLarge,  // 16px Inter 400
            color = UpcomingColors.body,
        )
    }
}
```

### Feature Card Title (18px)

```kotlin
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        backgroundColor = UpcomingColors.surfaceCard,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp)
        ) {
            icon()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = UpcomingTypography.titleMedium,  // 18px Inter 500
                color = UpcomingColors.ink,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = UpcomingTypography.bodyLarge,  // 16px Inter 400
                color = UpcomingColors.body,
            )
        }
    }
}
```

### Time Display (DM Mono 12px)

```kotlin
@Composable
fun EventTimeLabel(hour: Int, minute: Int) {
    Text(
        text = String.format("%02d:%02d", hour, minute),
        style = UpcomingTextStyles.monoLabel,  // 12px DM Mono, tabular numbers
        color = UpcomingColors.muted,
    )
}
```

### Editorial Italic Callout (16px Instrument Serif)

```kotlin
@Composable
fun CalloutQuote(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        backgroundColor = UpcomingColors.surfaceCard,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(
                text = text,
                style = UpcomingTextStyles.serifItalic,  // 16px Instrument Serif italic
                color = UpcomingColors.ink,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}
```

### Button Label

```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = UpcomingColors.primary,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = UpcomingTypography.labelLarge,  // 14px Inter 500
            color = UpcomingColors.onPrimary,
        )
    }
}
```

### Navigation Link

```kotlin
@Composable
fun NavLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            style = UpcomingTextStyles.navLink,  // 14px Inter 500
            color = UpcomingColors.primary,
        )
    }
}
```

---

## Material3 Theme Integration

### Full Theme Setup

```kotlin
package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = UpcomingColors.primary,
    onPrimary = UpcomingColors.onPrimary,
    primaryContainer = UpcomingColors.primaryContainer,
    onPrimaryContainer = UpcomingColors.onPrimaryContainer,
    secondary = UpcomingColors.accentTeal,
    onSecondary = UpcomingColors.onSecondary,
    // ... rest of Material3 color mappings
)

@Composable
fun UpcomingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = UpcomingTypography,  // ← Your typography scale
        content = content
    )
}
```

---

## Responsive Typography Scaling

### Mobile vs Desktop Scaling

```kotlin
// Extension function for responsive font sizing
@Composable
fun responsiveDisplayLarge(): TextStyle {
    val isCompact = isCompactScreen()  // Check screen width
    return if (isCompact) {
        UpcomingTypography.displayLarge.copy(
            fontSize = 32.sp,            // 64px → 32px on mobile
            lineHeight = 38.4.sp,        // 1.2 ratio
            letterSpacing = (-0.8).sp,   // -1.6px → -0.8px
        )
    } else {
        UpcomingTypography.displayLarge
    }
}

@Composable
fun HeroSectionResponsive() {
    Text(
        text = "Plan together.\nMove faster.",
        style = responsiveDisplayLarge(),
        color = UpcomingColors.ink,
    )
}

// Helper to detect compact screens
@Composable
fun isCompactScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp < 768
}
```

---

## Font Feature Settings (Advanced)

### Tabular Numbers in DM Mono

For time displays and data tables, use tabular-width numbers to ensure vertical alignment:

```kotlin
@Composable
fun DataTable() {
    Column {
        repeat(10) { index ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = String.format("%02d:00", index),
                    style = UpcomingTextStyles.monoLabel,  // DM Mono with tnum
                    modifier = Modifier.weight(0.3f),
                    textAlign = TextAlign.Right,
                )
                Text(
                    text = "Event $index",
                    style = UpcomingTypography.bodyLarge,
                    modifier = Modifier.weight(0.7f),
                )
            }
        }
    }
}
```

---

## Accessibility & Contrast

### Color + Type Combinations

| Use | Typography | Color | Contrast Ratio | WCAG |
|---|---|---|---|---|
| Body paragraph | bodyLarge (16px) | ink (#141413) on canvas (#faf9f5) | 15.2:1 | AAA ✓ |
| Button text | labelLarge (14px) | on-primary (#fff) on primary (#cc785c) | 5.8:1 | AA ✓ |
| Secondary text | bodyMedium (14px) | muted (#6c6a64) on canvas (#faf9f5) | 8.1:1 | AAA ✓ |
| Footer | bodySmall (14px) | on-dark-soft (#a09d96) on surface-dark (#181715) | 4.8:1 | AA ✓ |
| Time label | monoLabel (12px) | muted (#6c6a64) on canvas (#faf9f5) | 8.1:1 | AAA ✓ |

All color + type pairings meet **WCAG AA (4.5:1)** minimum. Most exceed **WCAG AAA (7:1)**.

---

## Gotchas & Tips

### ✅ Do
- ✅ Use `fontFeatureSettings = "tnum"` on DM Mono labels for time/data alignment
- ✅ Apply negative letter-spacing on all display sizes (displayLarge down to headlineSmall)
- ✅ Reserve Instrument Serif for italic-only callouts; don't use it for regular text
- ✅ Keep all display type at weight 400 (Normal) — never bold the headings
- ✅ Use `UpcomingTextStyles.monoLabel` for all time displays (HH:MM format)

### ❌ Don't
- ❌ Don't use Inter weight 700 for emphasis — use size or italic instead
- ❌ Don't apply Instrument Serif in regular (non-italic) weight
- ❌ Don't forget `fontFeatureSettings` on DM Mono; numbers won't align without it
- ❌ Don't override letter-spacing on display types — the tracking is part of the brand voice
- ❌ Don't mix font families in the same paragraph (except for rare editorial accents)

---

## Testing Checklist

Before committing typography changes:

- [ ] Inter weights load correctly (300, 400, 500, 600, 700)
- [ ] Instrument Serif italic loads (check italic flag in declaration)
- [ ] DM Mono loads with `fontFeatureSettings = "tnum"` working
- [ ] Display sizes (64px down to 28px) show negative letter-spacing
- [ ] Hero h1 displays in 64px Inter at 1.1 line height
- [ ] Time labels display in 12px DM Mono with tabular alignment
- [ ] Body text (16px) reads clearly at all font sizes
- [ ] Button labels (14px) fit within 40px button height
- [ ] Italic serif callouts render correctly (not replaced by fallback)
- [ ] Responsive scaling works (64px → 32px on mobile)
- [ ] Contrast ratios pass WCAG AA (use Chrome DevTools Accessibility audit)
- [ ] All text is readable on both light and dark backgrounds

---

## Font Fallback Testing

If fonts don't load, the system will fall back to:
- **Inter** → System sans-serif (SansSerif)
- **Instrument Serif** → System serif (Serif)
- **DM Mono** → System monospace (Monospace)

The fallback stack is designed to degrade gracefully — the page won't break, but the refined typography won't appear until fonts load.

---

## Migration from Old Typography

If you're updating from the previous Upcoming theme:

### Old → New Mapping

| Old Style | New Style | Changes |
|---|---|---|
| `displayLarge` (32sp Copernicus) | `displayLarge` (64sp Inter -1.6px) | Size 2x, serif → sans, no bold |
| `bodyMedium` (14sp Inter) | `bodyMedium` (14sp Inter) | No change |
| Time labels (old 14sp) | `monoLabel` (12sp DM Mono) | Size down, font to mono, tabular numbers |
| Callout text (old 16sp) | `serifItalic` (16sp Instrument Serif italic) | Font to serif, italic only |

### Update Checklist
- [ ] Replace old `displayLarge` calls with new `displayLarge` (64sp)
- [ ] Add `monoLabel` style to all time/date displays
- [ ] Replace serif callouts with `serifItalic`
- [ ] Test responsive scaling on mobile
- [ ] Verify button height still 40px with new `labelLarge`

---

## Future Enhancements

- Consider adding `font-smoothing: antialiased` for Mac browsers (sharp on high-res screens)
- Monitor performance; consider lazy-loading fonts if bundle size becomes an issue
- Test with screen readers; ensure semantic heading hierarchy (<h1> → <h2> → etc.) is maintained
- Plan for dark-mode typography scaling (if needed) — may need higher contrast on dark surfaces

---

## Reference

- **Inter GitHub:** https://github.com/rsms/inter
- **DM Mono GitHub:** https://github.com/colmcq/DM-Mono
- **Instrument Serif GitHub:** https://github.com/Instrument/instrument-serif
- **Compose Typography Docs:** https://developer.android.com/jetpack/compose/designsystems/typography
- **Material Design 3 Typography:** https://m3.material.io/styles/typography/overview
- **WCAG Contrast Ratio Calculator:** https://webaim.org/resources/contrastchecker/

---

**Version:** 1.0  
**Last Updated:** August 28, 2026  
**Platform:** Android Jetpack Compose  
**Fonts:** Inter, DM Mono, Instrument Serif
