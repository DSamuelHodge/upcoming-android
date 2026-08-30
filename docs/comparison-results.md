# Upcoming vs Cal.com-Android — Implementation Comparison

**Baseline:** `docs/research.md` — the Android "Book-A-Meeting" design plan that benchmarked Calendly and Cal.com and scoped a native Kotlin/Compose client (the "Cal.com Android version we architected and scoped").
**Compared against:** Upcoming for Android as actually built (this repo) plus its bespoke backend (`github.com/DSamuelHodge/upcoming-db`, Cloudflare Worker HTTP layer).
**Status:** updated August 30, 2026 — the stacked PRs through **Phase 8** are merged to `main` (Android #1–#5, upcoming-db #13–#17). Phase 8 shipped the Calendly-style Scheduling share surface (booking link + QR + single-use links) and the Glance home-screen widgets, which changes several verdicts below.
**Method:** every finding cites `file:line` in the implemented code. Severity of gaps: 🔴 must-close before "scheduling app" claim holds · 🟠 differentiating feature lost · 🟢 polish.

---

## 1. Verdict summary

| Research area (research.md §) | Status | One-line verdict |
|---|---|---|
| §2 Backend = Cal.com API v2 | 🔀 **Diverged (sanctioned fallback)** | Built the research's named fallback: bespoke `upcoming-db` (Turso/LibSQL + Drizzle) behind a Cloudflare Worker Hono HTTP layer — schema mirrors Cal.com's `EventType`/`Booking`/`Schedule`/`Availability` shape as the fallback prescribed |
| §3 Client architecture (modules, Hilt) | 🔀 Diverged | Same MVVM + Compose layering, but single `:app` module with packages instead of `:core:*`/`:feature:*` Gradle modules; no Hilt (manual DI) |
| §4 Auth (OAuth2+PKCE, social, biometric) | 🔀 Partially diverged | Email/password + JWT pair vs our own Worker; EncryptedSharedPreferences; single-flight refresh — no OAuth/PKCE, no social/magic-link, no biometric |
| §5 Views (Calendly IA) | ✅/➖ Partial | Dashboard/Event Types/Availability/Bookings/Booking flow/Settings all built; **Scheduling share tab added in Phase 8** (booking link + QR + single-use links); Contacts, meetings timeline, reschedule, host payment onboarding not built |
| §6 Payments (Stripe + Play Billing) | 🔀 Diverged, core flow works | Real Stripe book→pay→mark-paid with server-verified `paid`; but no PaymentSheet, no Connect destination charges, no Play Billing, no packages/invoices |
| §7 Notifications | 🔀 Partially diverged | Exact-alarm engine + multi-offset reminders + Room ledger built; but "FCM" is **locally simulated** (no Firebase Messaging), no boot re-arm, no WorkManager reconciler, no ICS, no inbox |
| §8 Widgets (Glance/QS tile/shortcuts) | 🔀 Partial | Phase 8 shipped 2x2 "Upcoming Widget" + 4x2 "Upcoming List" Glance widgets mirroring the dashboard, with deep links into Booking Detail; no QS tile, no app shortcuts, no Share-Link/Availability widget variants |
| §9 Data model (Room) | 🔀 Diverged by design | 8 entities mirroring the bespoke server schema 1:1 (stronger than research's list in host modeling); ISO-8601 UTC strings not epoch millis; no outbox/slots_cache/contacts |
| §10 Security & reliability | ➖ Mostly not built | Encrypted token storage + publishable-key-only APK done; no cert pinning, Play Integrity, Crashlytics, deep-link verification, R8 minify off |
| §11 Roadmap | ~50% of P0, ~40% of P1 | Collective/round-robin (a P2 item) shipped **early** via the bespoke backend; widgets/share/contacts (P1–P2) not started |
| — (not in research) | ➕ Beyond scope | Full cream/coral design system, server-backed Settings hub with BYO credentials, demo mode, legal/permissions screens, metrics dashboard |

---

## 2. The five big divergences

### 2.1 Backend: Cal.com API v2 → bespoke `upcoming-db` (the sanctioned fallback)

Research scoped slot computation, buffers, round-robin and webhooks as "solved" by Cal.com API v2, with a fallback of mirroring Cal.com's schema if a bespoke backend was mandated. The fallback was taken, and the result is a genuine scheduling engine rather than a thin client:

- **Engine:** `availability-engine.ts` is pure, DST-correct (walks day-by-day in each schedule's IANA timezone, handles spring-forward/fall-back), with multi-host routing (`individual`/`round_robin`/`collective`), slot-grid enforcement, min-notice, and buffer expansion — see `upcoming-db/docs/api-contract.md` §2.3.
- **Atomicity:** bookings commit through `createBookingHandler` with client idempotency keys, snapshotted buffers, and a `host_occupancy_ticks` unique index as the concurrency backstop (409 on double-book).
- **HTTP layer:** Hono Worker on Cloudflare (`worker.ts:145`–`worker.ts:815`) exposing `auth/*`, `event-types`, `availability`, `bookings`, `bookings/cancel`, `payments/*`, `me`, `me/credentials` — the `mapErrorToHttp` contract (409/404/400, generic 500) research never had to specify.
- **Integrations:** Daily.co room minting + teardown and Stripe PaymentIntents live server-side.

**What was lost vs Cal.com:** calendar busy/free sync (Google/MS Graph), webhooks/push relay, routing forms, workflows, OAuth clients, team/org administration. **What was gained:** full data ownership, no EE-license boundary (research risk #4 is moot), and the Android client's contract is exactly the repo's own `docs/api-contract.md`.

### 2.2 Auth: OAuth2+PKCE → email/password JWT

| Research scoped | Built |
|---|---|
| OAuth2 + PKCE via Custom Tabs; Google/Microsoft/magic-link | Email + password against `POST /auth/signup` / `/auth/login` (`UpcomingApi.kt:69-79`) |
| Cal.com's 60-min access / 1-yr refresh rotation | HS256 access token **1h** / refresh **30d** (`upcoming-db/auth.ts:24-25`), scrypt password hashing (`auth.ts:9-21`) |
| Keystore-backed storage, rotate-on-refresh, single-flight | `EncryptedSharedPreferences` w/ AES-256-GCM master key (`AuthTokenManager.kt:28-39`); single-flight refresh via OkHttp `Authenticator` that only replays when a JWT was sent (`UpcomingApi.kt:97-106`) |
| Biometric app lock, session revoke via FCM token | Not built. Logout does best-effort server revocation (`AuthRepository.kt:71-79`); refresh tokens are hashed server-side |
| Roles OWNER/INVITEE | Single signed-in persona + a **demo mode** (research never mentioned it) with strict demo-data purge on session establishment (`UpcomingRepository.kt:127-137`) |

The refresh model is compatible with what research demanded (rotate-on-refresh, atomic persist, single-flight); only the grant type and provider surface diverge. Google Sign-In dependencies are already staged in `gradle/libs.versions.toml:104-106` (commented out in `app/build.gradle.kts:109-114`).

### 2.3 Payments: right rails, different plumbing

Research's flow #1 (booking fees are Play-exempt real-world services → Stripe) was implemented, but:

- **Confirmed with the raw Stripe SDK**, not PaymentSheet: `createPaymentMethodSynchronous` + `confirmPaymentIntentSynchronous` from raw card fields (`UpcomingRepository.kt:866-895`). No Google Pay, no wallets, no 3DS-ready UI. PaymentIntent creation pins `automatic_payment_methods[allow_redirects]=never` server-side to make in-app confirmation work.
- **Sequence is book→pay→mark-paid:** the booking is created first (holding the slot), a PaymentIntent is minted per event-type price, the client confirms, and `POST /payments/mark-paid` flips `paid` **only after server-side PI verification** (`UpcomingRepository.kt:897-908`). This honors research's rule "client never confirms paid state itself" — achieved via mark-paid instead of a Stripe webhook.
- **Failure semantics are implemented and stricter than scoped:** definitive payment failure auto-cancels the held booking; network failure keeps it for retry; back-nav out of the payment step releases the slot (`InviteeBookingViewModel.kt:204-316`).
- **Missing:** Play Billing (research flow #2 — app subscriptions), packages/invoices (flow #3), Stripe Connect destination charges + host onboarding, refund mirroring. Card data does touch the app's own form (research wanted PaymentSheet to be the boundary), though it is tokenized client-side by the SDK before transport.

### 2.4 Notifications: the local half is solid, the push half is simulated

Built (`NotificationAndReminderManager.kt` + `UpcomingRepository.kt:282-351`):

- Exact alarms via `setExactAndAllowWhileIdle` with graceful degrade to inexact on `SecurityException` (`:188-204`) — research's #1 risk (Android 14+ exact-alarm cliff) is handled, plus `canScheduleExactAlarms()` (`:131-135`) surfaced in a dedicated Permissions screen.
- Multi-offset reminders per booking with stable per-(booking, offset) request codes (`:224-225`); a Room reminder ledger (`notification_reminders`, `Entities.kt:139-149`) for bookkeeping; cancel/reminders pruned atomically with booking cancellation (`UpcomingRepository.kt:811-851`).
- Offset settings persisted in DataStore **and** synced through `users.metadata.prefs` so they follow the account (`UpcomingRepository.kt:288-311`).

Diverged / missing:

- 🔴 **No real push.** `triggerFcmNotification` (`NotificationAndReminderManager.kt:97-128`) posts a *local* notification; there is no `firebase-messaging` dependency, no FCM service in `AndroidManifest.xml`, and the Worker has no webhook→push relay. The backend's `notifications.ts` sends emails, not pushes.
- 🔴 **No re-arm on reboot or periodic reconciliation.** The manifest registers only `AlarmReceiver` (`AndroidManifest.xml:32-34`); there is no `RECEIVE_BOOT_COMPLETED` receiver and no WorkManager periodic job, so alarms die on reboot/OEM force-stop until the next settings change or re-booking. `rescheduleAllReminders()` (`UpcomingRepository.kt:316-336`) already exists — it just is never called at launch.
- ➖ No ICS-with-VALARM payload, no notification-center inbox screen, no foreground "starting soon" service, no DND-aware quiet hours.

### 2.5 Structure: single module, manual DI

Research prescribed `:app` + `:core:*` + 8 `:feature:*` Gradle modules with Hilt. Built instead:

- One `:app` module with package-level separation (`core/{auth,database,designsystem,engine,model,network,prefs,repository}`, `feature/{auth,availability,bookingflow,bookings,dashboard,eventtypes,legal,notifications,permissions,settings}`).
- Manual DI at the composition root: `MainActivity.kt:36-50` builds DB → tokens → API → repositories; a single `ViewModelProvider.Factory` wires all ViewModels (`UpcomingNavigation.kt:389`).
- minSdk **24** (scoped 26), target 36 (`app/build.gradle.kts:18-19`).

The layering contract (UI → ViewModel → Repository → API/Room) is identical; the divergence costs compile-time isolation and build parallelism, not correctness.

---

## 3. Feature matrix (research §5 views + P0–P3 features)

| Feature (research ref) | Status | Evidence / note |
|---|---|---|
| Today/`Home` screen w/ metrics | ✅ | Dashboard w/ Revenue/Hours/Upcoming computed from live data |
| Booking detail (cancel, notes, payment status) | ✅ partial | `BookingDetailScreen.kt`; cancel ✅, notes ✅, **reschedule ➖, no-show ➖** |
| Event types + editor wizard | ✅ | `EventTypeEditorScreen.kt` w/ location menus, buffers, notices, pricing, colors |
| Availability editor (weekly grid + overrides + timezone) | ✅ | `AvailabilityScreen.kt`; weekly rules + date overrides + tz (server lockstep via `PATCH /me/schedule`); **not a drag grid** |
| Invitee booking flow (slot grid → form → pay) | ✅ | `InviteeBookingViewModel.kt`; 4-step flow; server-computed slots w/ invitee-tz display (`UpcomingRepository.kt:535-605`) |
| Share: booking link + QR, single-use links | ✅ **early** | Phase 8 Scheduling tab: personal link + per-type single-use links, QR dialog (ZXing, `core/util/QrCode.kt`), share sheet/copy/embed snippet; backend `single_use_links` table with **burn-on-booking** inside the booking tx (409 on reuse) — research placed single-use links in P2 |
| Deep-link invitee mode (App Links, assetlinks.json) | 🔀 Partial | Share URLs live on the official domain `https://getupcoming.app/{username}[/{slug}?lid={token}]` (`worker.ts:756`); the widget→Booking Detail deep link is internal (`EXTRA_WIDGET_BOOKING_UID`); no verified App Links / `assetlinks.json` into the app itself |
| Contacts (list/detail/book-with/CSV) | ➖ | Nothing built |
| Meetings timeline (day/week, filters) | ➖ | Bookings list is flat; no timeline/filters |
| Reschedule ("proposed new time" negotiation) | ➖ | Schema supports it; no flow |
| Group / collective / round-robin | ✅ **early** | All three in the engine + `strategy-collective` seed; research placed this in P2 |
| Stripe booking fees | ✅ diverged | §2.3 above |
| Play Billing subscriptions | ➖ | Not built (needs the research's legal ruling first) |
| Packages / invoices | ➖ | Not built |
| Real-time push (FCM) | ➖ simulated | §2.4 above |
| Local exact-alarm reminders, per-type matrix | ✅ | Offsets are account-level, not per-event-type (research wanted per-type matrix) |
| ICS w/ VALARM to invitee calendar | ➖ | Not built |
| Notification center inbox | ➖ | Settings→Notifications is toggles only |
| Glance widgets (Up Next / Share Link / Availability) | 🔀 | 2x2 "Upcoming Widget" + 4x2 "Upcoming List" shipped (Phase 8), both deep-linking to Booking Detail via `WidgetSnapshotStore` uids (`core/widget/UpcomingWidgets.kt:140,214`); Share-Link and Availability widget variants not built |
| QS tile (pause bookings) + app shortcuts | ➖ | Not built |
| Offline-first Room + outbox mutations | 🔀 | Room cache + offline **reads** ✅ (`UpcomingRepository.kt:72-75`); writes are remote-first with no outbox queue |
| Cert pinning / Play Integrity / Crashlytics / R8 | ➖ | None; `isMinifyEnabled = false` (`app/build.gradle.kts:45`) |
| OAuth calendar connect (Google/MS) | ➖ | Not built — availability is schedule-based only |

**Roadmap coverage estimate (post–Phase 8):** P0 ≈ 5/8 · P1 ≈ 5/10 · P2: collective/round-robin + timezone handling + **single-use links** done (3/8) · P3: 0.

---

## 4. What Upcoming has that research never scoped

1. **A complete design system** — cream/coral Anthropic-style tokens, Inter/DM Mono/Instrument Serif, implemented in `core/designsystem/Tokens.kt` + `ui/theme/*` and documented in `docs/upcoming-design-system.md` (with a typography implementation guide and comparison doc).
2. **Server-backed user settings**: profile/identity via `GET/PATCH /me`, timezone lockstep via `PATCH /me/schedule`, booking-defaults per location type, and **bring-your-own credentials** (`daily_api_key`, `ical_url`, `caldav_url`, `stripe_secret_key`) stored AES-256-GCM encrypted server-side with masked hints only (`UpcomingApi.kt:55-65`, Worker `worker.ts:562-640`).
3. **Demo mode** with guaranteed isolation: seed data never leaks into a signed-in session; cache purge + identity re-point on session establishment (`UpcomingRepository.kt:127-137`, `seedInitialDataIfEmpty()` gated on `isDemoSession()` at `:913-919`).
4. **Legal & permissions screens** (Terms/Privacy/Permissions) and a splash-screen auth gate.
5. **Device-verified end-to-end**: availability (DST-correct), booking with minted Daily room, 409 on double-book, PI confirm → mark-paid → cancel frees slot + room deleted — verified live on hardware against the deployed Worker.
6. **Demo-persona fence** (hardened in PR #4, commits `b0ac27a`/`41b9fb3`): demo seeding gated on `isDemoSession()` + a persisted flag; sign-in purges the cache (flag or demo-email detection → `clearAllTables()` → identity re-point → `/me`); all UI fallbacks neutralized so "Alex Rivera" can never render in a signed-in session. Device-verified: Room holds only the real account. (Server-seeded `demo-*` booking uids cached in Room are backend seed rows, not a demo-mode leak.)
7. **Phase-8 share surface + widgets**: official domain `getupcoming.app` personal links, single-use links with burn-on-booking, QR/share/embed, and the 2x2 + 4x2 Glance widgets with deep links — all built and device-verified beyond anything the research scoped for its P0/P1.

---

## 5. Recommendations (prioritized)

| # | Action | Closes | Severity | Effort |
|---|---|---|---|---|
| 1 | **Re-arm reminders at launch + boot + periodic reconciliation**: add `RECEIVE_BOOT_COMPLETED` receiver calling `rescheduleAllReminders()`; call it in `MainActivity`/`DashboardViewModel` init; optionally a 6h WorkManager job (research §7). The function already exists — only triggers are missing. | 🔴 P0 gap | S |
| 2 | **Real push**: add `firebase-messaging`, register the token with the Worker, forward booking-created/cancelled/paid events from the Worker (or replace simulated `triggerFcmNotification` calls once real push lands). | 🔴 P0 gap | M |
| 3 | **Reschedule flow** (proposed-new-time): reuse the booking flow against a new slot; schema and cancel path already support it. | 🟠 P1 | M |
| 4 | **Payment UX**: swap raw-card confirmation for Stripe **PaymentSheet** (+ Google Pay); keeps the mark-paid contract unchanged. Add Stripe Connect when hosts other than the platform owner exist. | 🟠 | M |
| 5 | **Auth surface**: enable the staged Google Sign-In deps, add biometric app lock (research §4); consider refresh-token rotation server-side. | 🟠 | M |
| 6 | **Engagement surface completion**: QS tile (pause bookings) + dynamic app shortcuts; Share-Link and Availability widget variants; verified App Links (`assetlinks.json`) so `getupcoming.app` links open the app directly. | 🟠 P1 | M |
| 7 | **Security hardening**: enable R8, cert pinning via `network_security_config`, Crashlytics; keep Play Integrity for booking-create until abuse is observed. | 🟡 | S/M |
| 8 | **Offline outbox** for booking creation (research §9) — needed only if offline booking becomes a requirement; current remote-first writes fail loudly instead. | 🟡 P2 | M |
| 9 | **Per-event-type reminder matrix** (upgrade account-level offsets to per-type, as Calendly workflows do). | 🟢 | S |
| 10 | **Structural**: split `:core:*`/`:feature:*` modules + Hilt **only if** team size/build times demand it; do not block features on it. | 🟢 | L |

**Effort key:** S <½ day · M ½–2 days · L >2 days.

---

## 6. Bottom line

Upcoming is a **native-client realization of the research's client half, running on the research's sanctioned fallback backend**. The scheduling core (slot grid, buffers, min-notice, multi-host, DST, idempotency, atomic conflict handling) and the paid-booking loop (book→pay→mark-paid with slot-holding semantics) are real and end-to-end verified. With Phase 8, the share/engagement surface research scoped for P1–P2 (booking links, QR, single-use links, home-screen widgets) is now built too — single-use links even get the stronger treatment (server-side burn-on-booking inside the booking transaction). The remaining gaps are concentrated exactly where research predicted the cliffs: **push delivery** and **reminder persistence across reboots**, plus the still-absent contacts/timeline/reschedule. None require re-architecture; recommendations 1–2 are the difference between "works while the app is alive" and "reliable scheduler."
