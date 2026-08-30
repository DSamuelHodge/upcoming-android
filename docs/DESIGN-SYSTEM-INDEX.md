# Upcoming Android — Design System Package

**Complete design system adapted from Claude's editorial aesthetic, featuring Inter + DM Mono + Instrument Serif typography.**

---

## 📚 Document Index

### 1. **upcoming-design-system.md** (Primary Reference)
**The complete, authoritative design system document.**

- Overview & philosophy (warm cream canvas + coral accent)
- Complete color palette with hex values
- Typography scale (Inter display, DM Mono data, Instrument Serif accents)
- Layout & spacing system (96px section rhythm)
- Component library (buttons, cards, inputs, badges, forms)
- Do's & Don'ts (brand guard rails)
- Responsive behavior & touch targets
- Known gaps & future scope

**Use this for:** Understanding the full system architecture, referencing token values, building new components.

---

### 2. **upcoming-android-typography-implementation.md** (Developer Guide)
**Code examples for Jetpack Compose — how to implement typography in Type.kt**

- Font setup & installation (`app/src/main/res/font/`)
- Complete Compose `Typography` object with all 17 type styles
- Real Compose component examples (hero, cards, buttons, time labels)
- Custom text styles (serifItalic, monoLabel, navLink)
- Material3 theme integration
- Responsive typography scaling (mobile vs. desktop)
- Font feature settings (tabular numbers for DM Mono)
- Accessibility & contrast verification
- Migration checklist (if updating from old system)
- Testing checklist

**Use this for:** Implementing typography in your Compose codebase. Copy-paste ready code.

---

### 3. **typography-comparison.md** (Evolution Guide)
**Before/after: Claude system → Upcoming system.**

- Design philosophy shift (serif-driven → sans-driven)
- Side-by-side scale comparison (all type sizes)
- Real-world renderings (hero, feature cards, time labels, callouts)
- Font loading impact & performance
- Responsive scaling breakdown
- Accessibility comparison (readability, dyslexia-friendly)
- Implementation effort (open-source fonts reduce complexity)
- Migration checklist

**Use this for:** Understanding why changes were made, explaining the shift to stakeholders, migrating from Claude system.

---

### 4. **Original Design Documents**

The supporting files from the Android extraction (created earlier):

- **DesignTokenSystem.md** — Extracted from original app (reference)
- **BrandImplementationGuide.md** — Implementation scenarios
- **design-tokens.json** — Machine-readable format
- **design-tokens.csv** — Spreadsheet format
- **design-tokens.md** — Quick reference

---

## 🎨 Quick Reference

### Typography Hierarchy

| Element | Size | Font | Weight | Tracking |
|---|---|---|---|---|
| **Hero H1** | 64px | Inter | 400 | -1.6px |
| **Section Head** | 48px | Inter | 400 | -1.2px |
| **Subsection** | 36px | Inter | 400 | -0.8px |
| **Card Title** | 18px | Inter | 500 | 0 |
| **Body (Default)** | 16px | Inter | 400 | 0 |
| **Time Label** | 12px | DM Mono | 400 | 0 (tabular) |
| **Button Label** | 14px | Inter | 500 | 0 |
| **Editorial Italic** | 16px | Instrument Serif | 400 | 0 |

### Color Palette

| Token | Value | Use |
|---|---|---|
| **Canvas** | #faf9f5 | Default page floor (cream) |
| **Primary** | #cc785c | Buttons, CTAs, accents (coral) |
| **Ink** | #141413 | Headlines, primary text (dark) |
| **Body** | #3d3d3a | Default running text |
| **Surface Dark** | #181715 | Product mockups, footer (navy) |
| **Success** | #5db872 | Task completion, availability |
| **Error** | #c64545 | Validation errors, warnings |

### Spacing

- Base: 4px
- xs: 8px · sm: 12px · md: 16px · lg: 24px · xl: 32px · xxl: 48px · section: 96px

### Border Radius

- md: 8px (buttons, inputs)
- lg: 12px (cards, features)
- xl: 16px (hero container)
- pill: 9999px (badges, buttons)

---

## 🚀 Getting Started (3 Steps)

### Step 1: Review the System
Read **upcoming-design-system.md** (~15 min) to understand:
- Color palette
- Typography scale
- Component library
- Brand voice ("composed, restrained, data-aware")

### Step 2: Implement Typography
Follow **upcoming-android-typography-implementation.md** to:
- Add fonts to `app/src/main/res/font/`
- Create font families in `Type.kt`
- Copy the `UpcomingTypography` object
- Test font loading & fallbacks

### Step 3: Build Components
Use **upcoming-design-system.md** component specs to:
- Create buttons (`button-primary`, `button-secondary`)
- Build cards (`feature-card`, `product-mockup-card-dark`)
- Form inputs & validation
- Responsive breakpoints

---

## 📋 Implementation Checklist

### Phase 1: Typography (Week 1)
- [ ] Download fonts (Inter, DM Mono, Instrument Serif)
- [ ] Add to `app/src/main/res/font/`
- [ ] Create font families in `Type.kt`
- [ ] Define `UpcomingTypography` object
- [ ] Update `UpcomingTheme` to use new typography
- [ ] Test display sizes (64px hero)
- [ ] Test responsive scaling (mobile → desktop)
- [ ] Verify contrast ratios (WCAG AA)

### Phase 2: Colors (Week 2)
- [ ] Create color tokens in `Color.kt` (or reference file)
- [ ] Update `LightColorScheme` in `Theme.kt`
- [ ] Test all UI against cream canvas (#faf9f5)
- [ ] Test dark surfaces (#181715)
- [ ] Verify button contrast on coral background
- [ ] Test accessibility (color blindness simulator)

### Phase 3: Components (Week 3–4)
- [ ] Button component library (`button-primary`, variants)
- [ ] Card components (`feature-card`, `product-mockup-card-dark`)
- [ ] Input & form components
- [ ] Badge & tag components
- [ ] Navigation component
- [ ] Test all on Android devices (phone + tablet)

### Phase 4: Polish (Week 5)
- [ ] Review spacing (96px section rhythm)
- [ ] Audit typography consistency
- [ ] QA responsive breakpoints (< 768px, 768–1024px, > 1024px)
- [ ] Final accessibility audit
- [ ] Screenshot comparisons (before/after)
- [ ] Deployment & rollout

---

## 🎯 Key Design Decisions

### Why Inter (not Copernicus)?
- Copernicus is Anthropic proprietary; Inter is open-source
- Sans display feels more "app," less "publication"
- Inter weight 400 (normal) reads as composed; bold feels bombastic
- Contemporary aesthetic for a scheduling tool

### Why DM Mono for time labels?
- Monospace creates visual "precision" signal
- Tabular numbers (font-variant-numeric: tnum) enable vertical alignment
- 12px DM Mono in a calendar grid is faster to scan than 14px sans
- Differentiates data from prose

### Why Instrument Serif italic (only)?
- Serif-only typography (95% sans, 5% serif) = editorial accent without dominating
- Italic callouts feel literary and distinguished
- Rare usage prevents design from feeling cluttered
- Reduces font loading burden (no serif display weight needed)

### Why cream canvas (#faf9f5)?
- Warm tone differentiates Upcoming from cool-gray AI tools
- Deliberately not pure white; creates visual warmth
- Pairs perfectly with coral (#cc785c) accent
- Consistent with Anthropic brand warmth

---

## ❓ FAQ

### Q: Where do I download the fonts?
**A:** All three fonts are open-source:
- **Inter:** https://github.com/rsms/inter (or Google Fonts)
- **DM Mono:** https://github.com/colmcq/DM-Mono (or Google Fonts)
- **Instrument Serif:** https://github.com/Instrument/instrument-serif

### Q: Can I use fallback fonts if I don't have time to install custom fonts?
**A:** Yes, but the design will lose refinement. Fallbacks are:
- Inter → system sans (Roboto on Android, SF Pro Display on iOS)
- Instrument Serif → system serif (Noto Serif)
- DM Mono → system mono (Roboto Mono)

The app will still work and be readable, but the typography won't feel as composed.

### Q: Do I need to support dark mode?
**A:** The current system is single-mode (cream canvas + dark surfaces for content). A global dark-mode toggle would invert the canvas to dark and surface-dark to light. This is future scope — plan separately if needed.

### Q: What's the font loading impact?
**A:** ~150kb total for all weights (Inter 5 weights, DM Mono 1 weight, Instrument Serif italic 1 weight). If fonts delay:
- Instrument Serif is used sparingly (5%) → minor loss if delayed
- Inter is critical → design degrades gracefully to sans fallback
- DM Mono is used only for time labels → secondary loss

Strategy: Load fonts async; render with fallbacks immediately.

### Q: How do I handle multiple languages?
**A:** Not in scope for this version. Inter has excellent multilingual support; Instrument Serif & DM Mono do not. Plan separately if expanding internationally.

### Q: Should I match the marketing site typography?
**A:** Not exactly. This system is app-focused; the marketing site (claude.com reference) can be different. But they should share the warm cream + coral color palette for brand consistency.

---

## 🔗 File Dependencies

```
upcoming-design-system.md
├── References colors from design-tokens.json
├── References typography hierarchy
├── Defines all components (button, card, input, etc.)
└── → Use for component specs

upcoming-android-typography-implementation.md
├── Implements Type.kt based on upcoming-design-system.md
├── Code examples for Jetpack Compose
├── References color tokens from design-tokens.json
└── → Use for coding

typography-comparison.md
├── Compares Claude system → Upcoming system
├── Explains design philosophy shifts
├── Migration guide for teams
└── → Use for stakeholder communication

design-tokens.json
├── Machine-readable format of all tokens
├── Color, typography, spacing, radius
└── → Use for tooling, automation, design tool exports

design-tokens.csv
├── Spreadsheet format of all tokens
└── → Use for Google Sheets, Excel, non-technical stakeholders
```

---

## 🛠️ Tools & Resources

### Font Resources
- **Google Fonts:** https://fonts.google.com (Inter, DM Mono)
- **GitHub:** https://github.com/rsms/inter, https://github.com/colmcq/DM-Mono
- **Font Feature Settings:** https://developer.mozilla.org/en-US/docs/Web/CSS/font-feature-settings

### Design & Accessibility
- **Material Design 3 Typography:** https://m3.material.io/styles/typography
- **Jetpack Compose Typography:** https://developer.android.com/jetpack/compose/designsystems/typography
- **WCAG Contrast Checker:** https://webaim.org/resources/contrastchecker/
- **Color Blindness Simulator:** https://www.color-blindness.com/coblis-color-blindness-simulator/

### Android Development
- **Jetpack Compose Docs:** https://developer.android.com/jetpack/compose
- **Material3 Components:** https://developer.android.com/jetpack/androidx/releases/compose-material3
- **Font Loading in Compose:** https://developer.android.com/jetpack/compose/designsystems/custom-fonts

---

## 📞 Support & Questions

- **Typography questions?** → See **upcoming-android-typography-implementation.md**
- **Color/component questions?** → See **upcoming-design-system.md**
- **Why these changes?** → See **typography-comparison.md**
- **Design system theory?** → See Claude's design reference (original inspiration)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 1.0 | Aug 28, 2026 | Initial system: Inter + DM Mono + Instrument Serif, adapted from Claude brand |

---

## Summary

**Upcoming Android's design system is a warm, composed, data-aware system built on:**

1. **Inter** — Contemporary humanist sans for display (64px down) and body (16px). Weight 400 for displays (quiet authority) and weight 500 for labels (clarity).
2. **DM Mono** — Monospace for time labels, dates, and data (12px). Tabular numbers enable vertical alignment. Signals precision.
3. **Instrument Serif** — Italic-only accent for editorial callouts (16px). Adds literary flavor without serif dominating the design.

**Colors:**
- **Canvas:** #faf9f5 (warm cream, default floor)
- **Primary:** #cc785c (warm coral, CTA accent)
- **Ink:** #141413 (dark warm, text)
- **Surface Dark:** #181715 (navy, product mockups)

**Voice:** Composed, restrained, data-aware. Warm but contemporary. For teams who get things done together.

---

**System Owner:** Upcoming Product Team  
**Last Updated:** August 28, 2026  
**Platform:** Android Jetpack Compose + Web Marketing  
**Derived from:** Anthropic/Claude Design System + Figma-first thinking
