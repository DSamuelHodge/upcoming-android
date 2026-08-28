# Upcoming Android — Design System

**Adapted from Claude's Geometric Balance + Anthropic warm-editorial aesthetic**

## Overview

Upcoming for Android inherits Claude's warmth but speaks in a more contemporary, restrained voice. The base atmosphere is a **tinted cream canvas** (`{colors.canvas}` — #faf9f5) — deliberately not the cool gray-white that every other productivity app uses. The display hierarchy runs **Inter** (a restrained humanist sans at weight 400) with tight negative letter-spacing, paired with **Instrument Serif** for occasional editorial moments and **DM Mono** for UI labels, data, and developer-facing copy.

The typography ladder is **deliberately quiet**: 64px hero copy in Inter weight 400 with -1.6px tracking reads as composed, not bombastic. The serif accent (Instrument Serif) is reserved for callout text and italicized editorials — it punctuates rather than dominates.

Brand voltage comes from the **cream + coral pairing** — coral (`{colors.primary}` — #cc785c) is the signature accent, used on every primary CTA, on full-bleed callout cards, and on key interaction moments. The coral is warm, slightly muted, never cyan/blue — a deliberate counter to the cool minimalism that dominates productivity apps.

### Visual Hierarchy Approach

The system has three surface modes that alternate:
1. **Cream canvas** (`{colors.canvas}`) — default application floor
2. **Light cream cards** (`{colors.surface-card}`) — feature cards, content sections
3. **Dark navy product surfaces** (`{colors.surface-dark}`) — calendar mockups, agent-task cards, data views, footer

The dark surfaces show Upcoming's product chrome — calendar grids, task hierarchies, event details, real data visualization rather than abstract illustrations. The cream-to-dark contrast sets the page's pacing rhythm.

**Key Characteristics:**
- Warm cream canvas (`{colors.canvas}` — #faf9f5) with dark warm-ink text (`{colors.ink}` — #141413). The brand's defining color choice.
- Coral primary CTA (`{colors.primary}` — #cc785c). Sparse on individual buttons, generous on full-bleed coral callout cards and interaction highlights.
- Restrained display hierarchy via Inter weight 400 at 64px hero scale with -1.6px tracking. Pairs with humanist sans (Inter body 400–500) for clarity.
- Instrument Serif weight 400 italic for editorial callouts and emphasized quotes — the "literariness" signal.
- DM Mono for data, labels, time displays, and any developer-facing copy — the "precision" signal.
- Dark navy product mockup cards (`{colors.surface-dark}` — #181715) carrying calendar grids, task lists, event previews — the app shows actual product chrome at scale rather than abstract.
- Light cream feature cards (`{colors.surface-card}` — #efe9de) — slightly darker than canvas, used for use-case explanations and capability highlights.
- Anthropic radial-spike mark — a small black 4-spoke radial — appears as the brand wordmark prefix.
- Border radius is hierarchical: `{rounded.md}` (8px) for buttons + inputs, `{rounded.lg}` (12px) for content + product cards, `{rounded.xl}` (16px) for hero illustration container.
- Section rhythm `{spacing.section}` (96px) — modern standard. Internal card padding stays generous at `{spacing.xl}` (32px).

---

## Colors

### Brand & Accent
- **Coral / Primary** (`{colors.primary}` — #cc785c): The signature Upcoming warm coral. Used on every primary CTA background, on full-bleed coral callout cards. The most-recognized Upcoming accent outside of the spike-mark logo.
- **Coral Active** (`{colors.primary-active}` — #a9583e): The press / hover-darker variant. Used on button press states and highlighted cards.
- **Coral Disabled** (`{colors.primary-disabled}` — #e6dfd8): A desaturated cream-tinted disabled state for inactive buttons.
- **Accent Teal** (`{colors.accent-teal}` — #5db8a6): Used on secondary interactive states, task completion indicators, "available time" blocks in calendar mockups.
- **Accent Amber** (`{colors.accent-amber}` — #e8a55a): A warm-tone companion used on priority badges, deadline warnings, and category highlights.

### Surface
- **Canvas** (`{colors.canvas}` — #faf9f5): The default application floor. Tinted cream — warm, deliberately not pure white.
- **Surface Soft** (`{colors.surface-soft}` — #f5f0e8): Section dividers, very-soft band backgrounds, subtle separation.
- **Surface Card** (`{colors.surface-card}` — #efe9de): Feature cards, content cards, use-case explanations. One step darker than canvas.
- **Surface Cream Strong** (`{colors.surface-cream-strong}` — #e8e0d2): A strongest-cream variant used on selected tabs, emphasized bands, and state indicators.
- **Surface Dark** (`{colors.surface-dark}` — #181715): Calendar mockups, task list cards, event detail cards, data tables, footer. The dominant dark surface for product display.
- **Surface Dark Elevated** (`{colors.surface-dark-elevated}` — #252320): Elevated cards inside dark bands (event detail panels in calendar mockups, expanded task details).
- **Surface Dark Soft** (`{colors.surface-dark-soft}` — #1f1e1b): Slightly lighter dark, used for secondary content inside larger dark cards (time zones, metadata).
- **Hairline** (`{colors.hairline}` — #e6dfd8): The 1px border tone on cream surfaces. Borders feel like one elevation step rather than ink lines.
- **Hairline Soft** (`{colors.hairline-soft}` — #ebe6df): Barely-visible divider used inside the same band, between rows in cards.

### Text
- **Ink** (`{colors.ink}` — #141413): All headlines, labels, and primary text. Warm dark, slightly off-pure-black.
- **Body Strong** (`{colors.body-strong}` — #252523): Emphasized paragraphs, lead text, task titles in dark cards.
- **Body** (`{colors.body}` — #3d3d3a): Default running-text color, body copy, event descriptions.
- **Muted** (`{colors.muted}` — #6c6a64): Sub-headings, breadcrumbs, secondary labels, time displays.
- **Muted Soft** (`{colors.muted-soft}` — #8e8b82): Captions, fine-print, metadata, copyright lines, inactive time slots.
- **On Primary** (`{colors.on-primary}` — #ffffff): Text on coral buttons and callout cards.
- **On Dark** (`{colors.on-dark}` — #faf9f5): Cream-tinted white used on dark surfaces (echoes the canvas tone).
- **On Dark Soft** (`{colors.on-dark-soft}` — #a09d96): Footer body text, secondary labels in dark mockups, inactive calendar slots.

### Semantic
- **Success** (`{colors.success}` — #5db872): Green status indicators, "task completed" dots, availability states.
- **Warning** (`{colors.warning}` — #d4a017): Warning callouts, deadline alerts, overdue task flags.
- **Error** (`{colors.error}` — #c64545): Validation errors, missed deadlines, blocking issues.

---

## Typography

### Font Family Stack

The system uses **three typefaces** in a deliberate hierarchy:

1. **Inter** (primary display + body)
   - Display headlines (hero h1, section heads)
   - Body copy (paragraphs, descriptions, UI labels)
   - Navigation, buttons, UI text
   - Weight 400 (regular) for display, 400–500 for body + labels
   - Weights available: 300, 400, 500, 600, 700

2. **Instrument Serif** (editorial accents)
   - Emphasized quotes, callout text
   - Italic body for editorial asides
   - Weight 400 italic only
   - Rare usage — punctuation only

3. **DM Mono** (data + precision)
   - Time displays (HH:MM, dates)
   - Data labels, metrics, API references
   - Code blocks (if present)
   - Weight 400 regular
   - Monospace grid alignment

**Fallback Stacks:**
- Display/Body: `Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`
- Editorial: `"Instrument Serif", "Tiempos Headline", Georgia, serif`
- Mono: `"DM Mono", "JetBrains Mono", monospace`

### Typography Scale & Hierarchy

| Token | Size | Weight | Font | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|---|
| `{typography.display-xl}` | 64px | 400 | Inter | 1.1 | -1.6px | Homepage h1, major hero ("Plan together. Move faster.") |
| `{typography.display-lg}` | 48px | 400 | Inter | 1.15 | -1.2px | Section headings, capability headlines |
| `{typography.display-md}` | 36px | 400 | Inter | 1.2 | -0.8px | Sub-section heads, use-case titles |
| `{typography.display-sm}` | 28px | 400 | Inter | 1.25 | -0.4px | Card headlines, pricing tier names |
| `{typography.title-lg}` | 22px | 500 | Inter | 1.3 | 0 | Pricing plan labels, event titles |
| `{typography.title-md}` | 18px | 500 | Inter | 1.4 | 0 | Feature card titles, intro paragraphs |
| `{typography.title-sm}` | 16px | 500 | Inter | 1.4 | 0 | List item labels, card section headings |
| `{typography.body-md}` | 16px | 400 | Inter | 1.55 | 0 | Default running-text, event descriptions |
| `{typography.body-sm}` | 14px | 400 | Inter | 1.55 | 0 | Footer body, secondary text, time zone info |
| `{typography.caption}` | 13px | 500 | Inter | 1.4 | 0 | Badge labels, captions, metadata |
| `{typography.caption-uppercase}` | 12px | 500 | Inter | 1.4 | 1.5px | Category tags, "NEW" badges, priority labels |
| `{typography.label}` | 12px | 500 | DM Mono | 1.4 | 0 | Time displays (HH:MM), date labels, data metrics |
| `{typography.button}` | 14px | 500 | Inter | 1.0 | 0 | Standard button labels, CTA text |
| `{typography.nav-link}` | 14px | 500 | Inter | 1.4 | 0 | Top-nav menu items, breadcrumb links |
| `{typography.serif-italic}` | 16px | 400 | Instrument Serif | 1.55 | 0 | Editorial asides, emphasized quotes in cards |
| `{typography.mono-data}` | 14px | 400 | DM Mono | 1.5 | 0 | API endpoints, config values, code inline |

### Design Principles

**Quiet Display Typography**
- All Inter display sizes (64px down to 28px) stay at weight 400. Never bold (700). The weight doesn't need to shout; the size and -1.6px tracking at hero scale convey hierarchy.
- Negative letter-spacing (-1.6px at 64px, scaling down to -0.4px at 28px) is essential to the brand voice. Without it, Inter reads as default; with it, it reads as intentional.
- The restrained approach — "quiet" — is the opposite of "BOLD CAPS HERO COPY." Upcoming's typography exhales rather than yells.

**Body Stays Neutral, Serif Punctuates**
- Body text (16px Inter 400) stays in the default humanist sans. It's warm and readable, never cold.
- Instrument Serif appears only in italic, only in callout blocks or editorial moments. A 16px italic serif quote in a feature card reads as authoritative and literary — the exception that proves the rule.
- Inter handles 95% of the UI. Serif is the 5% accent.

**Mono for Precision**
- DM Mono appears only for time displays (HH:MM), date labels, and data metrics. It signals "this is precise, this is data."
- 12px DM Mono for a time label inside a calendar grid is much faster to read than 12px Inter. The monospace grid alignment aids scanning.

**No Font Weight Overload**
- The system uses weight 400 for display (no 700 display heads), weight 400 for body (no 600 body), weight 500 for labels and buttons.
- Emphasis comes from size, italic, or color — never from weight escalation.

---

## Layout

### Spacing System
- **Base unit:** 4px.
- **Tokens:** `{spacing.xxs}` 4px · `{spacing.xs}` 8px · `{spacing.sm}` 12px · `{spacing.md}` 16px · `{spacing.lg}` 24px · `{spacing.xl}` 32px · `{spacing.xxl}` 48px · `{spacing.section}` 96px.
- **Section padding:** `{spacing.section}` (96px) vertical between major bands — modern-SaaS rhythm.
- **Card internal padding:** `{spacing.xl}` (32px) for feature cards, pricing tier cards, product mockup cards, use-case cards.
- **Callout / CTA bands:** `{spacing.xxl}` (48px) inside coral callout cards; 64px inside larger dark CTA bands.

### Grid & Container
- **Max content width:** ~1200px centered. Breathing room on desktop, full-bleed on mobile.
- **Editorial body:** Single 12-column grid; hero often uses 6-6 split (h1 + CTA left, calendar/task mockup right).
- **Feature card grids:** 3-up at desktop, 2-up at tablet, 1-up at mobile.
- **Pricing grid:** 3-up at desktop (Free / Pro / Team), 1-up at mobile.
- **Calendar preview grid:** Full-width mockup showing calendar grid with event cards.

### Whitespace Philosophy
The cream canvas + restrained Inter display + generous internal padding create an editorial pacing — Upcoming reads like a long-form magazine column rather than a dense productivity dashboard. Whitespace between bands stays uniform at 96px; whitespace inside cards is generous (32px), letting type breathe. Time displays and data metrics in DM Mono feel precise without feeling cramped.

---

## Elevation & Depth

| Level | Treatment | Use |
|---|---|---|
| Flat | No shadow, no border | Body sections, top nav, hero bands |
| Soft hairline | 1px `{colors.hairline}` border | Inputs, sub-nav, card edges |
| Cream card | `{colors.surface-card}` background — no shadow | Feature cards, content cards, use-case cards |
| Dark surface card | `{colors.surface-dark}` background — no shadow | Calendar mockups, task list cards, event detail cards |
| Subtle interaction | Faint shadow at low alpha on hover | Hover-elevated states (rarely used; `0 1px 3px rgba(20,20,19,0.08)`) |

The elevation philosophy is **color-block first, shadow rare**. Most depth comes from the cream-vs-dark surface contrast. Shadows are minimal. Dark surfaces have their own internal product chrome (calendar grid lines, event borders, time-zone labels) which adds detail without external shadows.

### Decorative Depth
- The Anthropic spike-mark glyph (4-spoke radial asterisk) appears as a small black mark in the brand wordmark.
- Calendar mockups carry their own internal product detail: grid lines in `{colors.hairline}`, event cards with shadows at `{colors.primary}` accent, time labels in DM Mono, day-of-week headers in Inter 500.
- Event detail cards inside dark surfaces show metadata (time, location, attendees) in smaller type with `{colors.on-dark-soft}`.
- No photography. Illustrations are minimal, line-art style with coral and dark-navy strokes on cream — hand-drawn feeling, never photorealistic.

---

## Shapes

### Border Radius Scale

| Token | Value | Use |
|---|---|---|
| `{rounded.xs}` | 4px | Badge accents, tiny dropdowns |
| `{rounded.sm}` | 6px | Small inline buttons, dropdown items |
| `{rounded.md}` | 8px | Standard CTA buttons, text inputs, tabs |
| `{rounded.lg}` | 12px | Content cards (feature, pricing, calendar-card, event-detail) |
| `{rounded.xl}` | 16px | Hero illustration container, marquee components |
| `{rounded.pill}` | 9999px | Badge pills, "NEW" tags, priority badges |
| `{rounded.full}` | 9999px / 50% | Avatar circles, icon buttons, attendee avatars |

### Illustration & Imagery
Upcoming's hero rarely uses photography. Instead:
- Simple line-art illustrations with coral + dark-navy strokes on cream canvas
- Calendar mockups (the dominant "hero" treatment on homepage) showing an actual calendar grid with event cards
- Task list mockups showing hierarchical task structure with completion indicators
- Event detail cards (dark surfaces) showing time, attendees, description with small avatars for attendees

When avatars are shown (testimonials or attendee lists), they crop to perfect circles at 40px diameter.

---

## Components

### Top Navigation

**`top-nav`** — Cream nav bar pinned to the top. 64px tall, `{colors.canvas}` background. Carries the Anthropic spike-mark + "Upcoming" wordmark at left in `{typography.nav-link}`, primary horizontal menu (Features, Solutions, Pricing, Resources, Company) in center-left, right-side cluster with "Sign in" text-link and "Try Upcoming" `{component.button-primary}` (coral).

---

## Buttons

### `button-primary`
- Background: `{colors.primary}` (#cc785c)
- Text: `{colors.on-primary}` (white), `{typography.button}`
- Padding: 12px × 20px
- Height: 40px (touch-friendly minimum)
- Border radius: `{rounded.md}` (8px)
- Hover/Active state: `{colors.primary-active}` (#a9583e), no shadow shift
- Used for: Primary CTAs, "Try Upcoming", "Sign Up" buttons

### `button-secondary`
- Background: `{colors.canvas}` (transparent cream)
- Text: `{colors.ink}`, `{typography.button}`
- Border: 1px `{colors.hairline}`
- Padding: 12px × 20px, height 40px
- Border radius: `{rounded.md}`
- Hover state: Background shifts to `{colors.surface-card}` (#efe9de)
- Used for: Secondary CTAs, "Learn More", "Explore"

### `button-secondary-on-dark`
- Background: `{colors.surface-dark-elevated}` (#252320)
- Text: `{colors.on-dark}` (cream white)
- Border: 1px `{colors.on-dark-soft}` (optional, subtle)
- Padding: 12px × 20px, height 40px
- Border radius: `{rounded.md}`
- Hover: Background slightly lighter
- Used for: Buttons overlaid on dark surfaces (calendar mockup CTAs, event detail actions)

### `button-text-link`
- Background: none
- Text: `{colors.primary}` (coral), `{typography.button}`
- Underline: on press only
- Used for: "Sign in" link, inline CTAs

### `button-icon-circular`
- Background: `{colors.canvas}`
- Border: 1px `{colors.hairline}`
- Size: 36px × 36px
- Icon: `{colors.ink}`, centered
- Border radius: `{rounded.full}` (50%)
- Used for: Share, "view more", close, expand actions

### `button-icon-dark`
- Background: `{colors.surface-dark-elevated}`
- Icon: `{colors.on-dark}` (cream)
- Size: 36px × 36px
- Border radius: `{rounded.full}`
- Used for: Actions inside calendar mockups, event detail close buttons

---

## Cards & Containers

### `hero-band`
- Cream canvas floor with 6-6 grid at desktop
- Left side: h1 in `{typography.display-xl}` (64px Inter -1.6px), sub-headline in `{typography.body-md}`, button row (primary + secondary)
- Right side: calendar or task mockup card, or minimalist line-art illustration
- Vertical padding: `{spacing.section}` (96px)
- No background; sits on `{colors.canvas}`

### `hero-illustration-card`
- Background: `{colors.canvas}` or `{colors.surface-dark}` depending on content
- Carries the calendar preview, task list preview, or coral-stroke illustration
- Border radius: `{rounded.xl}` (16px)
- Shadow: very subtle (0 1px 3px rgba(20,20,19,0.08)) on elevated state

### `feature-card`
- Used in 3-up feature grids
- Background: `{colors.surface-card}` (#efe9de — slightly darker cream)
- Border radius: `{rounded.lg}` (12px)
- Internal padding: `{spacing.xl}` (32px)
- Carries: small icon at top, headline in `{typography.title-md}`, description in `{typography.body-md}`, optional `{component.text-link}` at bottom

### `use-case-card`
- Background: `{colors.surface-card}`
- Border radius: `{rounded.lg}`
- Padding: `{spacing.xl}` (32px)
- Layout: icon left (40px), headline + description right
- Headline: `{typography.title-md}`, text: `{typography.body-md}`
- Icon: 40 × 40px in `{colors.primary}` or `{colors.accent-teal}`

### `product-mockup-card-dark`
- Dark navy card showing actual Upcoming product chrome (calendar interface, task list, event details)
- Background: `{colors.surface-dark}` (#181715)
- Border radius: `{rounded.lg}` (12px)
- Internal padding: `{spacing.xl}` (32px)
- Carries: calendar grid with event cards, task hierarchy, date labels in DM Mono, attendee avatars
- Labels in `{colors.on-dark}`, secondary text in `{colors.on-dark-soft}`

### `calendar-preview-card`
- Specialized dark product mockup showing a 4-week calendar grid
- Background: `{colors.surface-dark}`
- Grid lines: 1px `{colors.hairline}` on dark (appears as subtle divider)
- Event cards inside: `{colors.surface-dark-elevated}` backgrounds with coral accent border (2px left)
- Day headers: Inter 500 12px in `{colors.on-dark}`
- Time labels: DM Mono 12px in `{colors.on-dark-soft}`
- Event title: Inter 400 14px in `{colors.on-dark}`, time in DM Mono 12px muted

### `event-detail-card`
- Shown as a callout overlay or inline on dark surface
- Background: `{colors.surface-dark-elevated}` (#252320)
- Border radius: `{rounded.lg}`
- Padding: 16px
- Contains: event title (Inter 500 18px), time (DM Mono 14px), location, attendees (40px avatars), description (Inter 400 14px)
- Close button: `{component.button-icon-dark}` top-right

### `pricing-tier-card`
- Background: `{colors.canvas}` with 1px `{colors.hairline}` border
- Border radius: `{rounded.lg}`
- Padding: `{spacing.xl}` (32px)
- Plan name: `{typography.title-lg}` (22px Inter 500)
- Price: `{typography.display-sm}` (28px Inter 400, no serif — just Inter)
- Feature list: `{typography.body-md}` with checkmark icon
- CTA: `{component.button-primary}` at bottom

### `pricing-tier-card-featured`
- Featured tier (typically "Team" or "Pro")
- Background flips to `{colors.surface-dark}` (#181715)
- Text inverts to `{colors.on-dark}`
- The dark surface IS the featured-tier signal — no special badge needed

### `callout-card-coral`
- Full-bleed coral card carrying a major call-to-action
- Background: `{colors.primary}` (#cc785c)
- Text: `{colors.on-primary}` (white)
- Padding: `{spacing.xxl}` (48px)
- Border radius: `{rounded.lg}` (12px)
- Headline: `{typography.display-sm}` (28px Inter 400 -0.4px), sub-text: `{typography.body-md}` in on-primary
- CTA button: `{component.button-secondary-on-dark}` (cream button on coral) or icon button

### `task-list-card`
- Dark surface showing task hierarchy
- Background: `{colors.surface-dark}`
- Padding: `{spacing.lg}` (24px)
- Task items: indent hierarchy with small icons (checkbox, drag handle)
- Task title: Inter 400 16px, due date in DM Mono 12px
- Completed task: text strikes through, `{colors.on-dark-soft}`
- Optional: small colored tag at right for priority/category

---

## Inputs & Forms

### `text-input`
- Background: `{colors.canvas}`
- Text: `{colors.ink}`, `{typography.body-md}`
- Border: 1px `{colors.hairline}`
- Padding: 10px × 14px (vertical × horizontal)
- Height: 40px
- Border radius: `{rounded.md}` (8px)
- Placeholder text: `{colors.muted}`

### `text-input-focused`
- Border shifts to `{colors.primary}` (coral) or thickens
- Outer ring: 3px coral at 15% alpha
- Background stays `{colors.canvas}`

### `text-input-on-dark`
- Background: `{colors.surface-dark-elevated}` (#252320)
- Text: `{colors.on-dark}`
- Border: 1px `{colors.on-dark-soft}`
- Used for: search inputs in dark sections, filter inputs

### `select-dropdown`
- Same styling as text input
- Dropdown arrow: `{colors.muted}` icon on right side
- Expanded state: dropdown list with `{colors.canvas}` background, items highlighted on hover with `{colors.surface-card}`

### `date-input`
- Text input style with DM Mono font for date display (if pre-filled)
- Icon: calendar icon on right
- Clicking opens date picker (modal or inline)

### `time-input`
- Text input style with DM Mono font
- Format: HH:MM (monospace alignment)
- Icon: clock icon on right

### `checkbox`
- Size: 18 × 18px
- Unchecked: border 2px `{colors.hairline}`, background `{colors.canvas}`, rounded `{rounded.sm}` (6px)
- Checked: background `{colors.primary}`, icon (checkmark) in `{colors.on-primary}`
- Disabled: border and background both `{colors.primary-disabled}`

### `radio-button`
- Size: 18 × 18px
- Unchecked: border 2px `{colors.hairline}`, background `{colors.canvas}`, rounded `{rounded.full}`
- Checked: border 2px `{colors.primary}`, inner circle 10px `{colors.primary}`

---

## Tags / Badges

### `badge-pill`
- Small pill label for category tags
- Background: `{colors.surface-card}`
- Text: `{colors.ink}`, `{typography.caption}` (13px Inter 500)
- Padding: 4px × 12px
- Border radius: `{rounded.pill}` (9999px)

### `badge-coral`
- Coral-fill badge for "NEW", "BETA", featured highlights
- Background: `{colors.primary}` (#cc785c)
- Text: `{colors.on-primary}`, `{typography.caption-uppercase}` (12px Inter 500 with 1.5px tracking)
- Padding: 4px × 12px
- Border radius: `{rounded.pill}`

### `badge-priority`
- Used on tasks / events to show priority level
- **P0 (Urgent):** background `{colors.error}`, text white
- **P1 (High):** background `{colors.accent-amber}`, text `{colors.ink}`
- **P2 (Normal):** background `{colors.surface-card}`, text `{colors.ink}`
- **P3 (Low):** background `{colors.surface-soft}`, text `{colors.muted}`
- Type: `{typography.caption-uppercase}` (12px)
- Padding: 4px × 10px
- Border radius: `{rounded.sm}` (6px)

### `badge-status`
- Dot + label for task status or event status
- **Available:** dot `{colors.success}`, label "Available"
- **Booked:** dot `{colors.primary}`, label "Booked"
- **Pending:** dot `{colors.accent-amber}`, label "Pending"
- Type: DM Mono 12px + 8px dot (6px diameter)

---

## Tabs / Filters

### `category-tab` + `category-tab-active`
- Used in sub-nav rows (Solutions, Use Cases, Resources)
- **Inactive:** transparent background, text `{colors.muted}` (14px Inter 500)
- **Active:** background `{colors.surface-card}`, text `{colors.ink}`
- Padding: 8px × 14px
- Border radius: `{rounded.md}` (8px)
- Underline variant (optional): underline 2px `{colors.primary}` below active tab

---

## CTA & Footer

### `cta-band-coral`
- Pre-footer "Try Upcoming" call-to-action band
- Full-width cream canvas band (or use as card with rounded corners)
- Background: `{colors.primary}` (#cc785c)
- Text: `{colors.on-primary}` (white)
- Padding: 64px (vertical)
- Headline: `{typography.display-sm}` (28px Inter 400 -0.4px)
- Sub-line: `{typography.body-md}`
- CTA button: `{component.button-secondary-on-dark}` (cream button on coral background)

### `cta-band-dark`
- Alternative pre-footer CTA on developer / integration-focused pages
- Background: `{colors.surface-dark}` (#181715)
- Text: `{colors.on-dark}`
- Padding: 64px
- Left side: headline + sub-text; right side: code window mockup or task list preview

### `footer`
- Dark navy footer closing every page
- Background: `{colors.surface-dark}` (#181715)
- Text: `{colors.on-dark-soft}` for body, `{colors.on-dark}` for headings
- 4-column link list at desktop (Product / Company / Resources / Legal)
- Vertical padding: 64px top, 32px bottom
- Anthropic spike-mark + "Upcoming" wordmark in `{colors.on-dark}` at top-left
- Footer copyright in DM Mono 12px at bottom
- Footer never inverts (always stays dark)

---

## Do's and Don'ts

### Do
- ✅ Anchor every page on the cream canvas (#faf9f5). Pure white reads as "any other app"; the warm tint is the brand differentiator.
- ✅ Use Inter weight 400 for every display headline (64px down to 28px). Pair with Inter 400–500 body. Negative letter-spacing on display sizes (-1.6px at 64px) is non-negotiable.
- ✅ Use Instrument Serif italic (16px weight 400) only for editorial callouts and emphasized quotes. It punctuates the system, not dominates.
- ✅ Use DM Mono for all time displays (HH:MM), date labels, and data metrics. The monospace grid alignment aids fast scanning.
- ✅ Reserve `{colors.primary}` (coral #cc785c) for primary CTAs and full-bleed callout-card moments. Don't scatter coral elsewhere.
- ✅ Use `{component.product-mockup-card-dark}` and calendar mockups to show actual Upcoming product chrome. Don't use abstract illustrations when you can show real product.
- ✅ Pair `{component.feature-card}` (cream) with `{component.product-mockup-card-dark}` (navy) in alternating bands. The cream-to-dark rhythm is the pacing mechanism.
- ✅ Apply `{spacing.section}` (96px) between major bands for modern SaaS breathing room.
- ✅ Use the Anthropic spike-mark glyph as the brand wordmark prefix (black mark on cream or `{colors.on-dark}` on dark).

### Don't
- ❌ Don't use cool grays or pure white for canvas. Cream (#faf9f5) is the brand. Pure white breaks the editorial warmth.
- ❌ Don't bold serif display weight. Display stays at Inter 400 weight. Boldness (700) reads as bombastic.
- ❌ Don't use cool blue or saturated cyan as brand accent. The coral (#cc785c) is the voltage.
- ❌ Don't paint coral everywhere. Coral is scarce on individual buttons and generous only on full-bleed callout cards and primary CTAs.
- ❌ Don't use sans-serif for display headlines. Inter handles 95% of the typography hierarchy; serif (Instrument Serif) is the 5% accent.
- ❌ Don't use Instrument Serif in regular (non-italic). The serif is italic-only for editorials.
- ❌ Don't repeat the same surface mode in consecutive bands. Pacing alternates: cream → cream-card → dark-mockup → cream → coral-callout → dark-footer.
- ❌ Don't add hover effects beyond what's encoded. Primary button darkens on press; that's it. No glow, no scale-up, no slide effects.
- ❌ Don't use Inter weight 700 for body emphasis. Emphasis comes from size (title-md instead of body-md) or italic (Instrument Serif italic) or color — never weight escalation.

---

## Responsive Behavior

### Breakpoints

| Name | Width | Key Changes |
|---|---|---|
| Mobile | < 768px | Hamburger nav; display-xl hero 64px → 32px, tracking -1.6px → -0.8px; hero grid stacks (calendar mockup below h1); feature cards 1-up; pricing 1-up; footer 4-col → 1-col stack |
| Tablet | 768–1024px | Top nav stays horizontal but tightens; display sizes scale down 10%; feature cards 2-up; pricing 2-up |
| Desktop | 1024–1440px | Full top-nav with all menu items; display sizes at full spec; 3-up feature cards; 3-up pricing tiers |
| Wide | > 1440px | Same as desktop; max content width caps at 1200px; outer breathing room increases |

### Touch Targets
- `{component.button-primary}` minimum 40 × 40px ✓
- `{component.button-icon-circular}` exactly 36 × 36px (slightly under WCAG 44, but acceptable for centered icons)
- `{component.text-input}` height 40px ✓
- Task item entire card area is tappable; effective tap area >> 44px

### Collapsing Strategy
- **Top nav:** Collapses to hamburger at < 768px; menu opens as full-screen cream sheet.
- **Hero band:** 6-6 grid collapses to single-column on mobile — h1 + sub-head + buttons first, then calendar/task mockup below.
- **Feature grids:** Reduce columns (3 → 2 → 1) rather than scale cards down.
- **Pricing tiers:** Collapse from 3 to 2 to 1; featured-tier dark surface stays distinct at every breakpoint.
- **Calendar mockup:** Scales down but maintains grid legibility; time labels in DM Mono stay readable at 12px.

### Scaling Display Typography
- `{typography.display-xl}` (64px desktop) → 48px tablet → 32px mobile (maintain -1.6px tracking → -1.0px → -0.8px)
- `{typography.display-lg}` (48px) → 36px → 28px
- `{typography.display-md}` (36px) → 28px → 24px
- `{typography.display-sm}` (28px) → 24px → 20px
- Body and smaller stay constant across breakpoints

### Image Behavior
- Calendar mockups scale proportionally; grid lines stay 1px; text remains legible at 12px.
- Event cards inside calendars maintain padding; don't shrink drastically on mobile.
- Attendee avatars in event detail cards stay 40px diameter (or stack vertically if space constrained).
- Hero illustrations scale; coral strokes thin slightly on mobile.

---

## Component Interaction States

### Button States
- **Default:** Base style as defined
- **Hover:** Subtle background shift or 1px outline (no animation)
- **Active/Pressed:** `{colors.primary-active}` background for primary buttons
- **Disabled:** `{colors.primary-disabled}` background, cursor: not-allowed

### Input States
- **Default:** As defined (cream background, hairline border)
- **Focused:** 3px coral ring at 15% alpha, border shifts to coral
- **Filled:** Background stays `{colors.canvas}`, text `{colors.ink}`
- **Error:** Border shifts to `{colors.error}`, error message below input in `{colors.error}` text

### Card States
- **Default:** No shadow, color-block only
- **Hover:** Subtle shadow or border thickening (if defined)
- **Selected:** Border 2px `{colors.primary}` or background shift to `{colors.surface-card}`

### Task / Event States
- **Completed:** Text `{colors.on-dark-soft}`, strikethrough, icon checkmark
- **Overdue:** Border-left 3px `{colors.error}`, flag icon
- **In Progress:** Border-left 3px `{colors.primary}`, active indicator dot
- **Pending:** Border-left 3px `{colors.accent-amber}`, clock icon

---

## Typography Implementation Notes

### For Developers
- **Font loading:** Use a font loader (e.g., `@font-face` or Google Fonts API) to load Inter, Instrument Serif, and DM Mono. Specify weights needed:
  - Inter: 300, 400, 500, 600, 700
  - Instrument Serif: 400 (italic only needed)
  - DM Mono: 400 (regular only)

- **Negative letter-spacing:** On all display sizes (64px down to 28px), apply the tracking values in the typography table. This is essential to the brand voice.

- **Font fallbacks:** Provide full fallback stacks as documented above. Users without the fonts will see clean serifs (Tiempos/Garamond) or humanist sans (Roboto) — acceptable degradation.

- **DM Mono grid alignment:** Use `font-variant-numeric: tabular-nums` on DM Mono labels to ensure digits align vertically (important for time and date displays).

---

## Figma / Design Tool Setup

1. Create a type style library with all `{typography.*}` tokens
2. Export color styles for all `{colors.*}` tokens
3. Create component library for buttons, cards, inputs, badges with responsive variants
4. Document the cream canvas (#faf9f5) as the default artboard background
5. Use spacing tokens (4px base) to establish grid

---

## Known Gaps & Future Scope

- **Animation timings:** Transitions (fade, slide, reveal) are out of scope. Document separately if needed.
- **Form validation states:** Documented for focused state; error/success would need a live flow to confirm exact styling.
- **Accessibility (a11y):** The system defaults to WCAG AA (4.5:1 contrast on text). All color pairs meet this standard.
- **Dark mode toggle:** The system is inherently two-tone (cream + dark). A global dark-mode toggle would invert the entire canvas but is out of scope for this document.
- **Micro-interactions:** Hover effects, loading spinners, toast notifications are not formalized. Add as needed.
- **Localiz**ation:** Right-to-left (RTL) layout considerations not addressed. Plan separately if expanding internationally.
- **Platform-specific (Android/iOS):** This document is web-focused. Native app implementations would adapt the radius, spacing, and typography hierarchy to platform norms (e.g., larger touch targets for iOS).

---

## Summary

**Upcoming for Android** inherits Anthropic's warm-editorial warmth (cream canvas, coral accent) but speaks in a more restrained, data-focused voice through:

1. **Inter weight 400** at every display size — quiet, not bombastic. -1.6px tracking at 64px hero scale conveys sophistication without shouting.
2. **Instrument Serif italic** as a 5% accent for editorials and callouts — literariness without dominating.
3. **DM Mono** for all data, time, and precision moments — visual parsing aid, not filler.

The result is a system that feels composed, editorial, and focused on the actual product (calendars, tasks, scheduling) rather than abstract marketing speak. The pacing alternates cream → dark → coral, keeping the eye engaged and the hierarchy clear. Whitespace is generous, type is restrained, and every design decision reinforces that Upcoming is for people who care about time, people, and getting stuff done together.

---

**Design System Version:** 1.0  
**Last Updated:** August 28, 2026  
**Platform:** Android Jetpack Compose (web marketing site reference)  
**Derived from:** Claude Brand System + Anthropic Design Philosophy
