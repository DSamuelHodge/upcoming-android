# Checkpoint - 1

## Objective

- Refresh the Upcoming Android app to the documented design system (cream/coral, Inter/DM Mono/Instrument Serif) — done and committed.
- Integrate the app with the libSQL/Turso backend (github.com/DSamuelHodge/upcoming-db, [Daily.co](http://Daily.co) wired) via a thin Cloudflare Workers HTTP layer + real Stripe (test) payments, delivering one branch+PR per phase, verified end-to-end and deployed to the G63 device.

## Important Details

- User-approved architecture decisions: Room as offline read cache (network-first, Room fallback); additive schema extension in upcoming-db; Cloudflare Workers as API deploy target; Stripe in scope with flow book → pay → mark-paid (committed booking holds the slot; definitive payment failure auto-cancels; network failure keeps it for retry; back-nav out of payment step releases held booking).
- docs/[api-contract.md](http://api-contract.md) is authority: direct Turso = read-only sanctioned; writes/availability must go through handlers; mapErrorToHttp (409/404/400, generic 500).
- Worker live at [https://upcoming-db-api.dshodge2020.workers.dev](https://upcoming-db-api.dshodge2020.workers.dev) (wrangler authed as DSamuelHodge / dshodge2020 subdomain). Secrets set: API_SECRET=3408bc9c09b8e0908b71970d267b2d90ff55d8c7c86e06ba, TURSO_DATABASE_URL, TURSO_AUTH_TOKEN, DAILY_API_KEY (sourced from /Users/derrickhodge/Downloads/upcoming-db/.env), STRIPE_SECRET_KEY=sk_test_51SmRZaPufKwsPniX... (user-supplied test key; rotate when done).
- Android .env (gitignored → secrets plugin → BuildConfig): UPCOMING_API_BASE_URL=[https://upcoming-db-api.dshodge2020.workers.dev](https://upcoming-db-api.dshodge2020.workers.dev), UPCOMING_API_SECRET, STRIPE_PUBLISHABLE_KEY (pk_test only; secret key never in APK).
- Live DB upcoming-db-v2 seeded: user alex id 38, schedule 38 (Mon–Fri 09:00–17:00 ET), event types 38 (15min free), 39 (demo-30m free), 40 (deep-dive $75), **41** (strategy-collective$150); demo bookings ids 30/31/32 (demo-upcoming-paid-001, demo-upcoming-free-001, demo-past-collective-001) with host_occupancy_ticks for upcoming ones.
- Metrics logic (DashboardViewModel): Revenue = accepted+paid sum of event-type price; Hours = accepted minutes; Upcoming = getUpcomingBookingsFlow (all accepted ordered by startTimeUtc — no date filter).
- Stripe SDK usage (no Activity plumbing): Stripe(context, key).createPaymentMethodSynchronous(...) + confirmPaymentIntentSynchronous(...); Worker PI creation now includes automatic_payment_methods[allow_redirects]=never (required for in-app confirm).
- Build: gradle 9.3.1 on PATH (no wrapper), JDK 17; adb at /Users/derrickhodge/Documents/android-intelligence/.toolchains/android-sdk/platform-tools/adb; device serial 1010018024018888 (G63, portrait 720x1600).
- Design deviation (documented earlier): bodySmall = Inter 14sp per primary doc; emoji chips on dashboard cards replaced with outlined material icons; "Stripe Active" label dropped (icon-only chip).
- PRs intentionally left unmerged for user review.

## Work State

### Completed

- Design system refresh fully verified (compile/tests/Roboazzi/assembleDebug) and committed on main of upcoming-android; design markdowns moved to docs/; app deployed to G63 (old com.hodgeluke.upcoming removed; new com.aistudio.upcoming.kxmpzq installed).
- Dashboard emoji chips → material icons; "Stripe Active" → icon-only teal [Icons.Outlined.CreditCard](http://Icons.Outlined.CreditCard).
- PR #13 (upcoming-db, phase-1-schema-extension): additive columns (users.display_name/avatar_url, event_types.title/description/price_in_cents/currency/color_hex/is_active, bookings.paid/payment_intent_id/created_at, attendees.notes), applied live (12/12), drift OK (10 tables/70 cols/9 indexes). CI green.
- PR #14 (upcoming-db, phase-2-http-layer, stacked): worker.ts Hono layer (GET /health|/event-types|/bookings[/:uid]|/availability, POST /bookings|/bookings/cancel|/payments/create-intent|/payments/mark-paid), worker.test.ts (48 pass/0 fail overall), SchedulingType widened with individual, makeTxRepository exported, created_at stamped in handler, wrangler.toml (nodejs_compat), allow_redirects=never fix (committed+pushed+deployed).
- PR #1 (upcoming-android, phase-3-network-layer): core/network/ Retrofit+Moshi client, DTOs per contract, bearer auth, ApiException mapping; repository network-first with Room cache; idempotencyKey = UUID; .env.example keys.
- PR #2 (upcoming-android, phase-4-booking-pay-flow, stacked): book→pay→mark-paid, stripe-android 21.19.0, auto-cancel semantics, createPaymentIntent/confirmStripePayment/markBookingPaid in repository.
- Phase 5: Worker deployed + 5 secrets; live DB seeded (seed-live.ts); full E2E verified over prod: availability (DST-correct), booking with minted Daily room, 409 on double-book, PI confirm succeeded → mark-paid paid:true → cancel frees slot + Daily room deleted (API 404).
- Dashboard/EventTypes/Bookings ViewModels now call refreshEventTypes()/refreshBookings() in init (was a gap); on-device dashboard rendered live Turso data.
- Demo bookings seeded (seed-bookings.ts); on-device metrics verified: Upcoming 3 / Hours 2.0h / Revenue $225.
- Book-crash fixes implemented + built + installed: added KotlinJsonAdapterFactory to UpcomingApiClient.moshi (was: Unable to create converter for AvailabilityResponseDto crash), removed throwing ErrorMappingInterceptor (was: non-IO exception escaping on OkHttp Dispatcher thread → FATAL), added apiCall {} wrapper translating HttpException/IOException → ApiException in-coroutine, and full-replace refreshEventTypes (deletes local ids absent remotely — local seeds ids 1–4 conflicted with cloud ids 38–41, causing 404 event_type 1 not found).

## Active

- Debugging regression after latest rebuild/reinstall: app UI stuck at initial state (null-user fallbacks "JD"/"EST" from DashboardScreen.kt:57/267/294, 0 metrics, empty event types) though device DB (databases/upcoming_scheduling.db, pull via run-as) has 2 users, 8 event types (purge of local 1–4 didn't run → refreshEventTypes never completed), 8 bookings. Init chain appears blocked before loadDashboardData()'s combine (which would emit instantly); no FATAL in logcat. Just captured /tmp/opencode/check2.png after 12s — not yet analyzed.
- Uncommitted on phase-4-booking-pay-flow: UpcomingApi.kt, UpcomingRepository.kt, DashboardViewModel.kt, EventTypesViewModel.kt, BookingsViewModel.kt.
- upcoming-db working tree untracked: seed-live.ts, seed-bookings.ts.

## Blocked

- On-device Book-flow E2E re-verification is blocked by the stuck-UI regression above.

## Next Move

1. Analyze /tmp/opencode/check2.png; then diagnose the hang: add Log.d breadcrumbs (or break apart seedInitialDataIfEmpty() → refresh calls) in DashboardViewModel.init to find which step blocks; suspects: apiCall block behavior, Room write contention from three concurrent VM inits, or refreshEventTypes stalling before its purge.
2. Once UI populates: tap Book on a cloud event type (id 38–41) and verify availability loads without crash; then commit the bug-fix batch on phase-4-booking-pay-flow (updates PR #2), and report status (PRs remain open pending user's merge decision; remind to rotate the Stripe test key).

## Relevant Files

- /Users/derrickhodge/orca/upcoming-android (branch phase-4-booking-pay-flow) — active repo.
- app/src/main/java/com/example/core/network/UpcomingApi.kt — Moshi w/ KotlinJsonAdapterFactory, apiCall wrapper, auth interceptor; ErrorMappingInterceptor removed.
- app/src/main/java/com/example/core/repository/UpcomingRepository.kt — network-first repo; apiCall-wrapped calls; full-replace refreshEventTypes; upsertBookingRow, storeRemoteBooking, schedulePostBookingNotifications, payment methods.
- app/src/main/java/com/example/feature/dashboard/DashboardViewModel.kt, feature/eventtypes/EventTypesViewModel.kt, feature/bookings/BookingsViewModel.kt — refresh wiring (uncommitted).
- app/src/main/java/com/example/feature/bookingflow/InviteeBookingViewModel.kt, InviteeBookingScreen.kt — book→pay→mark-paid flow + StripePaymentStepView.
- gradle/libs.versions.toml, app/build.gradle.kts — stripeAndroid = "21.19.0" added.
- .env / .env.example — API base URL/secret, publishable key.
- /Users/derrickhodge/orca/upcoming-db (branch phase-2-http-layer) — worker.ts, worker.test.ts, wrangler.toml, schema.ts, schema.sql, apply-additive-2026-08-28.ts, seed-live.ts, seed-bookings.ts.
- /Users/derrickhodge/Downloads/upcoming-db/.env — live Turso/Daily credentials source.
- docs/[api-contract.md](http://api-contract.md) — handler contract authority.
- /tmp/opencode/check1.png, check2.png, metrics*.png, et2.png, flow.png — on-device screenshots for the current debug session.


# Checkpoint - 2

## Objective

- Deliver a full-stack User Settings feature for the Upcoming Android app (designed from docs/api-contract.md), as stacked PRs on both repos: upcoming-db /me endpoints + users.metadata contract; upcoming-android Settings hub with nested Notifications.
- Iterative refinement round (current): (a) expanded timezone list, (b) Booking Defaults with per-type Label+value and a default selector, (c) bring-your-own API keys / private URLs section (Daily.co, iCal, CalDAV, Stripe). Availability-rules write-sync and JWT/custom-domain/API-key-hosting are explicitly out of scope.
Important Details
- API auth: Worker bearer API_SECRET=3408bc9c09b8e0908b71970d267b2d90ff55d8c7c86e06ba; live at https://upcoming-db-api.dshodge2020.workers.dev. Secrets now include TOKEN_ENCRYPTION_KEY (openssl rand -hex 32, saved at /tmp/opencode/tok.txt) for AES-256-GCM credential storage.
- PR stack (all unmerged, awaiting user review): upcoming-db #13 → #14 → #15 (phase-6-user-settings-api, base phase-2-http-layer); upcoming-android #1 → #2 → #3 (phase-6-user-settings-ui, base phase-4-booking-pay-flow). Stripe test key rotation reminder still outstanding.
- Metadata contract (user-metadata.ts, strict Zod): locations (LocationDefaults: integrations:daily / inPerson / userPhone, each a passthrough LocationEntry), defaultLocationType enum, legacy defaultLocation fallback, prefs.timeFormat (12h/24h), role, company (role/company added because live seed data has them). resolveDefaultLocation() derives effective default.
- Credential types (worker-validated): daily_api_key, ical_url, caldav_url, stripe_secret_key. Endpoints: GET /me/credentials (masked hints only — plaintext never leaves server after write), PUT /me/credentials/:type (replaces; URL types require http(s)), DELETE /me/credentials/:type (404 if absent). Stored in credentials table via encryptToken.
- Repository tracks server identity: primaryUserId: MutableStateFlow(1L) re-pointed by refreshMe(); getPrimaryUserFlow() uses flatMapLatest (@OptIn ExperimentalCoroutinesApi). Fixes watching hardcoded local seed user 1 vs server id 38.
- Live DB user 38 profile now reads Derrick Hodge / hodge@agentmail.to / username derrick (user edited it); live metadata: locations {daily: "My Daily Room", inPerson: Office/123 Main St, userPhone: Mobile/+15555550999}, defaultLocationType=integrations:daily, role "Product Lead", company "Upcoming Labs". Test credentials stored: daily_api_key (dsh-test-key-abcd1234), ical_url (fake example) — replaceable.
- Build/deploy env unchanged: gradle 9.3.1 on PATH, JDK 17; adb at /Users/derrickhodge/Documents/android-intelligence/.toolchains/android-sdk/platform-tools/adb; device serial 1010018024018888 (G63, 720x1600); app id com.aistudio.upcoming.kxmpzq; DB pull via adb exec-out run-as com.aistudio.upcoming.kxmpzq cat databases/upcoming_scheduling.db.
- Tooling gotchas learned: BSD sed lacks \b (use python3 for in-place edits); worker.test.ts authed(path, init) helper — first arg is PATH, HTTP method goes in init (authed("", { method: "PATCH", ... })); on-device floating overlay widget near top-right (~(683,357)) can intercept row-radio taps — dialog path is reliable.

## Work State

### Completed

- Prior fixes verified by user; committed: bug-fix batch on phase-4-booking-pay-flow (cbd79a0, updates PR #2) and seed scripts on phase-2-http-layer (f49fa24, updates PR #14). The earlier stuck-UI regression is fully resolved.
- Settings design review done (plan mode): found Settings tab was actually NotificationsScreen with hardcoded toggles, no /me endpoints, users.metadata as extension point, credentials table unused.
- Phase A (upcoming-db): user-metadata.ts contract; GET /me, PATCH /me (409 on email/username clash, luxon IANA tz validation), PATCH /me/schedule (transaction keeps schedules.timezone + users.timezone in lockstep; creates schedule row if missing); PR #15 created; deployed + smoke-tested (found and fixed role/company rejection, restored live metadata).
- Phase B (Android): DTOs (MeResponseDto, PatchMeRequest, PatchScheduleRequest, UserMetadataDto, UserPrefsDto, ScheduleDto); UpcomingApi /me methods; repository refreshMe()/updateProfile()/updateTimezone(); Room UserEntity already had metadata (no migration).
- Phase C (Android): DataStore enabled (libs.androidx.datastore.preferences uncommented); core/prefs/UserPreferences.kt (3 notification toggles); toggles wired to gate schedulePostBookingNotifications (alarm+reminder row on tenMin, FCM on push); feature/settings/SettingsScreen.kt + SettingsViewModel.kt per docs/upcoming-design-system.md tokens; navigation rework (SETTINGS tab = Tune icons, NOTIFICATIONS = "settings/notifications", dashboard link → SETTINGS); NotificationsScreen persisted; editor prefill from default location (EventTypesViewModel.defaultLocation, EventTypeEditorScreen).
- Identity fix: primaryUserId + flatMapLatest in repository; SettingsViewModel init hardened with runCatching { refreshMe() }.
- PR #3 created (upcoming-android); verified on G63: settings render from live /me, timezone round-trip both ways (Europe/London → server → restored America/New_York), push toggle persisted across force-stop. PR #2/#14 updated earlier with fix batch + seeds.
- Refinement round backend (committed+pushed+deployed, commit 1e29eff on phase-6-user-settings-api, PR #15 updated): metadata gains locations map + defaultLocationType (+resolveDefaultLocation); /me/credentials PUT/GET/DELETE with masking; tests 70 total / 56 pass / 0 fail; TOKEN_ENCRYPTION_KEY secret set; live smoke OK (put daily key + ical, masked list, metadata roundtrip).
- Refinement round Android (built + device-verified, NOT yet committed): LocationsMapDto (+entryFor), CredentialHintDto, PutCredentialRequest, DeleteCredentialResponse; API credential methods; repository updateLocationDefault(type, location, makeDefault), setDefaultLocationType, new defaultLocation() resolver, credentialHints()/putCredential()/deleteCredential(); SettingsViewModel new state/actions; SettingsScreen rewritten: ~70 timezones + search filter, Booking Defaults with three LocationDefaultRows (own Label+value, RadioButton default) + LocationEditDialog, Integrations card with 4 CredentialSpecs + CredentialDialog (PasswordVisualTransformation, stored hint, Remove); timezone dialog has search field.
- On-device verification of refinements: Booking Defaults renders live server data; Integrations shows masked hints (••••1234 / ••••.ics); Phone configured via dialog (Mobile / +15555550999) + made default → server verified userPhone + entry saved; bug found+fixed: "Make default" button condition was inverted (isDefault → !isDefault); rebuilt, Video dialog "Make default" → server default flipped to integrations:daily, snackbar "Settings saved". Unit tests + assembleDebug green.
Active
- Android refinement-round changes are uncommitted on phase-6-user-settings-ui (UpcomingApiModels.kt, UpcomingApi.kt, UpcomingRepository.kt, SettingsViewModel.kt, SettingsScreen.kt; also docs/session-checkpoint-1.md tracked earlier).
- Final timezone-search check: last screenshot /tmp/opencode/tzsearch.png captured after typing "Tokyo" in the timezone dialog search — result not yet confirmed/reported.
- Minor: duplicate Europe/Stockholm in COMMON_TIMEZONES is handled by .distinct().

## Blocked

- None. (Row RadioButton taps near the top-right may be swallowed by the device floating overlay — the in-dialog "Use as default"/"Make default" paths are verified working, so not blocking.)

## Next Move

1. Confirm /tmp/opencode/tzsearch.png shows the filtered "Asia/Tokyo" result (timezone search working).
2. Commit + push the Android refinement round on phase-6-user-settings-ui (updates PR #3), then report to the user: all three requests implemented and live-verified; PR stack (#13→#14→#15, #1→#2→#3) open for review; remind to rotate Stripe test key and to replace the two test credentials on the server.

## Relevant Files

- /Users/derrickhodge/orca/upcoming-db (branch phase-6-user-settings-api) — user-metadata.ts (contract + resolveDefaultLocation), worker.ts (/me, /me/schedule, /me/credentials routes), worker.test.ts (70 tests), deployed via npx wrangler deploy.
- /Users/derrickhodge/orca/upcoming-android (branch phase-6-user-settings-ui) — active repo, uncommitted round-2 changes.
- app/src/main/java/com/example/feature/settings/SettingsScreen.kt — rewritten: COMMON_TIMEZONES (~70 + search), CREDENTIAL_SPECS, LocationDefaultRow/LocationEditDialog/CredentialDialog.
- app/src/main/java/com/example/feature/settings/SettingsViewModel.kt — locations/defaultLocationType/credentialHints state + actions.
- app/src/main/java/com/example/core/repository/UpcomingRepository.kt — primaryUserId identity, refreshMe/updateProfile/updateTimezone/updateLocationDefault/setDefaultLocationType/defaultLocation()/credential methods, prefs-gated notifications.
- app/src/main/java/com/example/core/network/UpcomingApiModels.kt / UpcomingApi.kt — /me + credentials DTOs and endpoints.
- app/src/main/java/com/example/core/prefs/UserPreferences.kt — DataStore notification prefs.
- app/src/main/java/com/example/feature/notifications/NotificationsScreen.kt — persisted toggles, nested under Settings.
- app/src/main/java/com/example/feature/eventtypes/EventTypesViewModel.kt + EventTypeEditorScreen.kt — default-location prefill for new event types.
- app/src/main/java/com/example/navigation/UpcomingNavigation.kt — SETTINGS/NOTIFICATIONS routes, bottom-nav rework.
- app/build.gradle.kts — datastore-preferences dependency enabled.
- /Users/derrickhodge/orca/upcoming-android/docs/api-contract.md, docs/upcoming-design-system.md — authority docs for contract and styling.
- /tmp/opencode/*.png — device verification screenshots (settings3, step2, final, tzsearch, etc.).
- /tmp/opencode/tok.txt — TOKEN_ENCRYPTION_KEY value (already set as wrangler secret).


&nbsp;