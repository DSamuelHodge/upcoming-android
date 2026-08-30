# Upcoming Android → Production Roadmap

**Scope:** what it takes to ship Upcoming on Google Play + make it agent-native via AppFunctions.
**Timeline assumption:** 6–8 weeks end-to-end if 2–3 engineers + concurrent backend work.
**Domain:** `getupcoming.app` (link shortener, web landing, recovery flows, credential reset).

> **Progress (2026-08-30):** the **backend lane is complete** (upcoming-db PRs #19–#23) — rate limiting (WAF rule + worker tiers, verified live), FCM server-side push + `*/15` reminder cron, event-type mutations, official API domain **`api.getupcoming.app`**, ops runbook, Turso delete protection. FCM server infrastructure was provisioned 2026-08-30 (project `getupcoming`-`prod`, SA key, `wrangler secret` uploaded; send path verified — see Phase 0 → "FCM setup"): the only remaining push work is **client-side**. All Android-lane items remain open — they are the critical path now. Authoritative HTTP + push contracts: backend repo `Docs/api-contract.md` §4.
> **AppFunctions close-out (2026-08-30):** Phase 3 runtime shipped (`f999d05`); the automated E2E suite is committed **opt-in-gated** (skipped by default); on-device E2E **verification deferred** to an android-36.1+ emulator image — root cause + resume path in Phase 3 → "Testing AppFunctions".

---

## Phase 0: Release Blockers (Must-Have Before Play Submission)

These **block Play Store approval** or create immediate post-launch fire:

### Security Hardening (1.5 weeks, blocks submission)
- [ ] **R8 minification & obfuscation** enabled (`app/build.gradle.kts`)
  - Impact: app size –30%, reverse-engineering defense
  - Test: verify release APK size & APK Analyzer inspection
- [ ] **Certificate pinning** for `api.getupcoming.app` (Workers custom domain attached 2026-08-30 — pin THIS, never the `*.workers.dev` hostname: Cloudflare rotates workers.dev edge certs without notice, which would brick pinned installs)
  - Use Network Security Config (`res/xml/network_security_config.xml`) + public-key pinning
  - Fallback chain: primary cert → secondary → disable on dev-signed APK
  - Rotate certs via app update (no offline fallback to unpinned)
  - Validate on the internal testing track before promoting the pin to production
- [ ] **Crash & error reporting** (Crashlytics OR Bugsnag)
  - Non-blocking errors logged + aggregated; fatal crashes surface in console
  - PII scrubbing: no user emails, booking UIDs, tokens in logs
  - Test: throw a test exception on a debug build
- [ ] **No hardcoded credentials in code or BuildConfig**
  - Secrets via Gradle secrets plugin ✓ (already done)
  - Verify: no `UPCOMING_API_SECRET` hardcoded in source
  - **⚠️ Strengthened (2026-08-30):** the Worker treats that secret as **admin** (`authIsAdmin`) — shipping it in any APK hands full admin to anyone who unzips it. Release builds must drop `UPCOMING_API_SECRET` entirely and remove the `?: apiSecret` fallback in `UpcomingApiClient` (JWT-only). Demo mode: debug-only, fully local (Room seeds, no network calls).
- [ ] **Play Integrity API** integration (replaces SafetyNet)
  - Hook into auth: `verifyPlayIntegrity()` on signup/login; reject non-certified devices if stricter policy desired
  - Non-blocking (logging only at MVP)
- [ ] **Network security policy** (Play requirement)
  - CleartextTrafficPolicy: `domain cleartextPermitted="false"` except localhost (testing)
  - Verify via Network Monitor in Android Studio

**Effort:** ~60 eng-hours (split: minify 8h, pinning 12h, Crashlytics setup 10h, Play Integrity 15h, policy audit 5h)

### Firebase Cloud Messaging (2 weeks, unblocks real push)
- [~] **FCM setup — server/infra side DONE 2026-08-30; client wiring open**
  - ~~Create Firebase project~~ ✅ **`getupcoming-prod`** created via gcloud (project 189422075971); Firebase added via Management API; `fcm.googleapis.com` + `firebaseinstallations.googleapis.com` + Management API enabled; **Google Cloud SDK installed** at `~/google-cloud-sdk` (account hodgedomain@gmail.com)
  - ~~Download 
`google-services.json` → `app/`~~ ✅ Android app registered (`appId 1:189422075971:android:e9b00c3429b5bcdb2594f6`, package app.getupcoming); config fetched via REST into 
`app/google-services.json` — **gitignored** (public repo; re-fetch via Firebase console or the REST `/config` endpoint)
  - [ ] Add `com.g
oogle.gms:google-services` gradle plugin (`libs.versions.toml` already pins googleServices 4.5.0; the plugin line in `app/build.gradle.kts` is commented out)
  - Infra notes: SA **`fcm-push@getupcoming-prod.iam.gserviceaccount.com`** holds `roles/firebasecloudmessaging.admin`; key JSON at `~/.config/upcoming/fcm-push-sa.json` (0600, outside all 
repos); **send path verified** with a dummy-token send (expected 400 INVALID_ARGUMENT); `wrangler secret put FCM_SERVICE_ACCOUNT` uploaded to `upcoming-db-api` — push is now LIVE server-side
  - [ ] Link project to Google Play Console (needs Play Console access)
- [ ] **Push token lifecycle** (`NotificationAndReminderManager`)
  - Listen to `onNewToken()` → **`PATCH /me` with `metadata.fcmToken`** (camelCase, on `/me` — NOT `/me/credentials`; contract: backend `Docs/api-contract.md` §4.4. The backend `UserMetadata` is strict, so this deployed first — 2026-08-30)
  - Server stores in `users.metadata.fcmToken`; overwrites on token refresh (one token per user for v1); server clears it automatically when FCM reports the token unregistered
- [ ] **Server-side push** (`upcoming-db/worker.ts`) — **✅ DONE (2026-08-30, backend #22)**
  - Implemented via FCM HTTP v1 + WebCrypto RS256 service-account JWT (the `firebase-admin` SDK is Node-only and cannot run on Workers — do not use it here)
  - Routes: lifecycle pushes (booking created/cancelled/paid → host) via `waitUntil`; reminder sweep on `*/15` cron + `POST /push-reminders` (admin, for staging tests)
  - Payload contract + token storage (`users.metadata.fcmToken`): backend repo
 `Docs/api-contract.md` §4.4
  - ~~Remaining: Firebase
 project + service account, then `wrangler secret put FCM_SERVICE_ACCOUNT`~~ ✅ **DONE 2026-08-30** (project `getupcoming`-prod, SA + role + key, secret uploaded; send path verified)
- [ ] **Client-side receiver** (`NotificationAndReminderManager`)
  - `FirebaseMessagingService.onMessageReceived()` → map `action` → local alarm (if local time-based reminder still firing) or direct notification
  - Foreground (app open): silent (let local alarm fire); background: show notification
- [ ] **Test & staging** 
  - ~~Staging worker route for manual push test~~ — **✅ exists: `POST /push-reminders` (admin-only, backend #22)**; returns `{"sent":N,"checked":N}`; no-ops without `FCM_SERVICE_ACCOUNT`
  - [ ] Device farm test: send from console, verify on real device

**Effort:** ~80 eng-hours (split: FCM client 15h, server 20h, token lifecycle 12h, testing 20h, docs 5h)

**Why urgent:** real push makes booking updates (cancels, payments) reach users who aren't in the app. Local-only reminders work today, and purely local notifications do pass store review — this is a product-quality call, not a policy blocker. (Clarified 2026-08-30.)

---

### Persistent Reminders (Boot + WorkManager) (1.5 weeks)
- [ ] **RECEIVE_BOOT_COMPLETED** receiver + WorkManager reconciler
  - Add `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />` + receiver in manifest
  - On boot, `BootCompletedReceiver` enqueues `ReminderReconciliationWorker` (10min delay for system settle)
  - Worker: query all non-cancelled bookings → query DataStore offsets → compare Room `notification_reminders` ledger → re-arm missing alarms
- [ ] **Periodic re-arm fallback**
  - Optional: weekly `PeriodicWorkRequest` (every 7 days) to catch alarm-clock resets or system clock jumps
  - Reduces re-boot dependency; helps with user-initiated time changes
- [ ] **Alarm degrade gracefully**
  - Catch `SecurityException` for `setExactAndAllowWhileIdle()` (Doze mode, some ROMs); fall back to `set()` (inexact)
  - Log level: warning (not fatal)
- [ ] **Tests**
  - `WorkManagerTest` with TestDriver: schedule reconciliation, advance clock, verify alarms re-armed
  - E2E: flash test device, reboot, verify reminder fires at scheduled time

**Effort:** ~40 eng-hours

---

### Play Store Submission Prerequisites (1 week)
- [ ] **App signing key** (Play App Signing)
  - Create upload key, let Play Store manage keystore
  - Store upload key securely (bitwarden / 1password); never commit to repo
- [ ] **Privacy Policy** (hosted at `getupcoming.app/privacy`)
  - Cover: user data (email, timezone, bookings, event types), storage (Room, preferences), network (Stripe, Daily.co, Cloudflare), retention (delete on logout)
  - Legal review (if in-house counsel available)
- [ ] **Terms of Service** (hosted at `getupcoming.app/terms`)
  - Payment terms (Stripe), cancellation, liability, GDPR data subject rights
- [ ] **Content rating questionnaire** (Google Play Console)
  - Rate as educational/productivity (no violence, sexual, etc.)
- [ ] **Screenshots + description**
  - 5–8 screenshots in 3–5 languages (English minimum)
  - Description: "Schedule meetings, manage availability, send calendar links"
  - Keywords: scheduling, calendar, booking, meetings
- [ ] **Google Play Console account & developer profile**
  - Sign with verified email; upload identity docs if needed (some regions)

**Effort:** ~30 eng-hours (legal + compliance + assets)

---

## Phase 1: MVP Production (Weeks 2–3 after Phase 0)

Once blockers clear, stabilize the experience for launch day:

### Performance & Stability
- [ ] **Network resilience audit**
  - Review `apiCall {}` error paths: is every 5xx/timeout properly surfaced?
  - Add exponential backoff + jitter for retry-able 429/503 (currently no retry on backoff, see `:138-159`)
  - Test: monkey-patch OkHttp to drop every 3rd request; verify graceful degradation
- [ ] **Memory profiling**
  - Compose recomposition: enable Layout Inspector, record trace of dashboard load
  - Room: verify no N+1 queries in flow subscriptions
  - Stripe SDK: check memory footprint in payment flow
- [ ] **Crash regression testing**
  - Instrumented tests on emulator (API 28, 30, 35) + physical device (API 24 minSdk, API 33 median)
  - Roborazzi screenshot regression suite passes on main branch
- [ ] **Analytics setup** (optional, Firebase Analytics)
  - Log user signup → login → booking flow; identify drop-offs
  - Non-invasive: no per-user ID tracking beyond anonymized session ID

**Effort:** ~40 eng-hours

### Backend Hardening
- [ ] **Worker observability** — *partial (2026-08-30)*
  - ✅ Structured JSON logging exists (`src/logger.ts`, one JSON object per line; FCM logs `fcm_send_failed` / `fcm_send_error` / `fcm_token_cleared` with user context)
  - [ ] Real-time alerts: 5xx error rate > 1%, slot-conflict rate spike > 10%
- [x] **Database backups** — **✅ DONE (2026-08-30)**
  - Turso PITR is platform-side (no job to enable); verified via export + point-in-time restore drill steps
  - Restore procedure documented: backend repo `Docs/ops-runbook.md` §2
  - Delete protection **enabled** on the production instance
- [x] **Rate limiting** — **✅ DONE (2026-08-30, backend #21+#22)**
  - Cloudflare WAF rule on `api.getupcoming.app`: 15 req/10s per IP per colo (authoritative flood ceiling, verified live)
  - Worker tiers: `/auth/*` 10/min, `/availability` 50/min, `POST /bookings*` + `/payments/*` 20/min, default 100/min (per-isolate best-effort backstop; DO-based global limiter is a post-launch upgrade)
- [x] **Idempotency key window extension** — **✅ already satisfied; no change needed (verified 2026-08-30)**
  - `bookings.idempotency_key` UNIQUE → duplicate POSTs replay the stored booking (covered by worker tests: "replays by idempotency key")
  - The stored row *is* the cache — no 24h TTL to add

**Effort:** ~30 eng-hours

---

## Phase 2: Production Operations (Week 4)

### Domain & Deep Links
- [ ] **getupcoming.app landing** — *partially unblocked (2026-08-30): the zone is active in Cloudflare and `api.getupcoming.app` → Worker is live; the `getupcoming.app` landing page itself is still to build*
  - Redirect `/` → Play Store link
  - Host privacy & terms (already documented)
  - Credential reset link: `/reset-password?token=...` (future OAuth gate, for now email-only)
- [ ] **Deep linking from email**
  - Booking confirmation email includes `getupcoming.app/bookings/{uid}` link
  - Android App Links: declare intent filter, host `getupcoming.app`, auto-verify
  - `NotificationAndReminderManager` uses deep links in notification intents
- [ ] **QR code support** (future feature, scope separately)
  - For now, skip; priority: text links
- [ ] **Short link service** (optional, Bit.ly / TinyURL)
  - Shorten confirmation links for SMS fallback (future)

**Effort:** ~20 eng-hours

### Monitoring & On-Call
- [x] **Runbook creation** — **✅ DONE (2026-08-30): backend repo `Docs/ops-runbook.md`** — covers "bookings failing" (health → `wrangler tail` → Turso status), rate-limit 429 triage, push-not-arriving (incl. the `/push-reminders` test call), backup/restore drill, secrets inventory + rotation
- [ ] **On-call rotation**
  - Engineer on PagerDuty; Crashlytics & alert thresholds trigger page
  - SLA: P1 (auth/bookings broken): 15min; P2 (reminders delayed): 1h
- [ ] **Postmortem culture**
  - Blameless postmortem template; publish to team Slack
- [ ] **Runbook link in README** (internal)

**Effort:** ~15 eng-hours

---

## Phase 3: Agent-Native via AppFunctions (Weeks 5–6)

**Goal:** enable a copilot (Claude, ChatGPT, etc.) to help users manage their Upcoming account programmatically.

> **Status (2026-08-30): SUBSTANTIALLY COMPLETE — as-built differs from this plan.** Shipped via **AndroidX AppFunctions** (on-device discovery, Android 16+; commit `f999d05`), NOT the HTTP-manifest + remote-executor design below. The on-device framework replaces the manifest, the `appFunctionCall` executor, and most server-side plumbing: KSP generates the schema (`upcoming_app_function_service.xml`), the service is manifest-registered with the `android.app.appfunctions.*` intent filters, and 6 functions map onto `UpcomingRepository` (which hits the existing worker routes — no backend changes needed, as this plan anticipated). Unit tests: `AppFunctionMappersTest`. **Still open:** on-device E2E verification with a real assistant (automated suite built, opt-in gated — see "Testing AppFunctions" for the deferral root cause + resume path), per-user rate limiting, and a product decision on the HTTP manifest for non-on-device copilots.
>
> **Shipped function surface (6, deliberately read-mostly):** `listEventTypes`, `getUpcomingBookings`, `getBooking`, `checkAvailability`, `createSingleUseBookingLink`, `getPersonalShareLink`. Note the design delta vs. the plan below: **no `createBooking`/`cancelBooking`** — an assistant mints a single-use link and the invitee books through it, so an agent can never commit a booking or destroy a host's slot directly. Signed-in gate: JWT session required, demo mode rejected (`requireSignedIn()`).
>
> The original design is kept below for reference/decision history:
>
> **Decisions (2026-08-30, owner-confirmed):**
> 1. **E2E = emulator path** — no physical Android 16+ device available; verification runs on an emulator (see Testing AppFunctions). **Concluded 2026-08-30:** the available API 36.0 image (May 2025, AppSearch module `360527520`) is too old for the alpha10/11 stack — deferral root cause + resume path in "Testing AppFunctions".
> 2. **Per-user rate limiting stays post-launch** (per-IP WAF + worker tiers are the shipped enforcement).
> 3. **Agent access scoped to on-device agents.** AppFunctions is the Android 16+ surface; devices below 16 (Blu G63 / Android 13 via Termux) cannot use it — the chosen path for those and for remote agents is an **MCP server and/or a CLI** talking to the existing API (new workstream — see "MCP/CLI agent surface" below).
> 4. **Design delta confirmed:** no `createBooking`/`cancelBooking` agent functions — assistants mint single-use links and the invitee books through them.
>
> **MCP/CLI agent surface (new workstream, decision #3):** expose Upcoming's scheduling capabilities (same read-mostly surface as the AppFunctions service — availability, bookings, single-use link minting) over **MCP** for remote agents (Claude Desktop, ChatGPT connectors) and/or as a **CLI** installable in Termux (Android 13) that authenticates against the existing worker API. Open questions to settle before build: (a) MCP server hosting — Cloudflare Worker (`/mcp` endpoint on the existing worker, streamable HTTP) vs standalone; (b) auth — scoped token vs user JWT per session; (c) CLI distribution (Termux package vs single-binary script). Effort estimate: MCP server on the worker ~30–40h; thin CLI reusing the API ~15–20h.

### AppFunctions Schema & Discovery

- [x] **Design AppFunctions surface** — ✅ shipped 6 read-mostly functions (see status note; no createBooking/cancelBooking by design)
- [x] **Authentication layer for AppFunctions** — ✅ via requireSignedIn() (JWT session; demo rejected). Server-side `X-Client-Type` header + scoped tokens: not implemented (marked optional/future in this plan)
- [x] **Worker route implementations** — ✅ N/A by design: functions map to existing routes via `UpcomingRepository`; no `app-function.ts` marshalling module needed (the AndroidX framework + KSP handle schema/marshalling client-side)
- [x] **Prompt injection defense** — ✅ typed Kotlin parameters via KSP schema, explicit validation (`rangeEndUtc` after start, positive limits), server-side Zod validation unchanged, errors never expose internals
- [ ] **Rate limiting for AppFunctions** — per-IP WAF + worker tiers apply (calls ride existing routes); **per-user limits (DO-based) remain open** — post-launch upgrade

### Client-Side AppFunctions Discovery & Execution

- [x] **AppFunctions manifest** — ✅ superseded by on-device discovery: KSP generates `upcoming_app_function_service.xml` + `upcoming_app_metadata.xml`; no HTTP manifest is needed for on-device assistants. **OPEN DECISION:** host `https://api.getupcoming.app/.well-known/appfunctions.json` anyway if non-on-device copilots (server-side Claude/ChatGPT agents hitting the API directly) are in scope — nothing is hosted there today (401 from the auth middleware).
- [x] **Execution flow** — ✅ superseded: no `UpcomingRepository.appFunctionCall` executor needed; `UpcomingAppFunctionService` methods call the repository directly under the AndroidX framework
- [x] **Scope enforcement** — ✅ signed-in-only gate, demo blocked; server-side owner-scoped queries unchanged (JWT `authUserId`); agents operate only on the authenticated user's data

### Testing AppFunctions

- [x] **Unit tests** — ✅ `AppFunctionMappersTest` (mapper coverage, committed with f999d05)
- [~] **E2E / on-device verification — DEFERRED (concluded 2026-08-30).** Automated suite exists and is committed: `AppFunctionServiceE2eTest` (discovery of all 6 functions + signed-out rejection via `AppFunctionManager`), **opt-in gated** so the default suite stays green: `-Pandroid.testInstrumentationRunnerArguments.appFunctionsE2e=true`.
  - **Why it can't run on the current image:** the API 36.0 emulator image (build `BE2A.250530.026.F3`, AppSearch mainline `360527520`, May 2025) predates the alpha10/11 stack by ~15 months. Its indexer cannot parse the v2 dynamic-schema XSD and falls back to the v1 XML property, which the compiler cannot emit — verified inside the 1.0.0-alpha11 compiler artifact (`appfunctions:generateV1Xml` is parsed but read by no processor; the `-v1.xml` generation is wired only in unreleased androidx-main). The image also lacks the `adb shell cmd app_function` verification commands, and its index job runs on a ~5-min cadence. The experimental v1-compat workarounds (gradle XML rewrite task + v1 manifest property) were **reverted at close-out** — the shipped manifest (`.schema` + `.v2` properties) is the official pattern and needs no changes.
  - **Resume path (no app changes needed):** create an AVD from `system-images;android-36.1;google_apis;x86_64` (rev 4), boot, then `adb shell cmd app_function list-app-functions | grep app.getupcoming` for the deterministic check, and run the gated suite with the flag above.
- [ ] **Copilot integration test** — open: same as above with an actual assistant end-to-end

**Effort:** ~120 eng-hours (split: schema design 10h, server impl 30h, client executor 20h, manifest 5h, testing 30h, docs 15h, copilot integration 10h)

---

## Phase 4: Post-Launch Features (Week 7+)

Lower priority; customer-requested or roadmap-driven:

- [ ] **Reschedule booking** (POST `/bookings/{uid}/reschedule`)
- [ ] **Share booking link** (generate short URL, send via SMS/email)
- [ ] **Calendar export** (iCal `.ics` download, via new route `GET /bookings/{uid}.ics`)
- [ ] **Google Calendar / Outlook sync** (OAuth, background sync)
- [ ] **Biometric lock** (Android BiometricPrompt for fingerprint auth)
- [ ] **Offline write queue** (Room outbox table, WorkManager for sync on reconnect)
- [ ] **Batch invites** (CSV upload, send calendar links)

---

## Summary: Work Breakdown by Role

### Android Engineer (2–3 people, 6–8 weeks)
- **Phase 0 Security:** minify, pinning, Crashlytics, Play Integrity (50h)
- **Phase 0 FCM:** client receiver + token lifecycle (40h)
- **Phase 1 Boot/WorkManager:** reconciliation (40h)
- **Phase 1 Performance:** profiling + instrumented tests (40h)
- **Phase 3 AppFunctions:** ~~client executor + tests~~ ✅ substantially done via AndroidX AppFunctions (commit `f999d05` — service, 6 functions, mappers + unit tests; E2E suite committed opt-in-gated, on-device verification deferred to an android-36.1+ image — see Phase 3 "Testing AppFunctions")
- **Phase 0 Submission:** screenshots, privacy, signing (30h)
- **Total:** ~250 eng-hours (~8 weeks @ 30h/week)

### Backend Engineer (1–1.5 people, 6–8 weeks) — **lane COMPLETE 2026-08-30 (incl. FCM infra)**
- **Phase 0 Security:** ~~rate limiting~~ ✅ done (backend #21/#22 — WAF rule + worker tiers)
- **Phase 
0 FCM:** ~~server-side push, token storage~~ ✅ done (backend #22 + infra 2026-08-30: Firebase project, SA key, `wrangler secret` — see Phase 0 "FCM setup")
- **Phase 1 Hardening:** ~~rate limits, backups~~ ✅ done; observability partial (structured logging exists; alerting open)
- **Phase 2 Operations:** ~~runbook~~ ✅ done (`Docs/ops-runbook.md`); monitoring/on-call open
- **Phase 3 AppFunctions:** schema, server impl, manifest (60h) — open
- **Total:** ~155 eng-hours (~5–6 weeks @ 30h/week, can overlap with Android)

### Product/Design (0.5 person, 2–3 weeks)
- **Phase 2 Domain:** landing page, deep link strategy (15h)
- **Phase 3 AppFunctions:** UX guidance, test scripts (10h)

---

## Risks & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Slot conflict race conditions undetected in load testing | Medium | High | Stress test with 50 concurrent bookings/sec; monitor 409 rate post-launch |
| FCM token invalidation cascades (old tokens build up) | Low | Medium | ✅ Stronger than planned (implemented 2026-08-30, backend #22): a token clears from `users.metadata.fcmToken` on the **first** 404/403/410 bounce — no 5-strike counter needed |
| Reminders don't fire after app uninstall/reinstall | Medium | Medium | Test fresh install → first booking → reminder → uninstall → reinstall → booking still shows → manual arm on boot |
| WorkManager jobs orphaned if app force-stopped | Low | Low | Document in runbook; suggest users restart app after reboot |
| Copilot misinterprets availability response (slot format ambiguous) | Medium | Medium | Include `description` fields in JSON response; schema includes exemplar values |
| Timezone DST edge case mishandled in booking + reminder | Low | High | Add test case: book at 2am EDT during spring-forward (2am becomes 3am); verify reminder fires at correct UTC moment |

---

## Go/No-Go Decision Checklist

**Before Play Store submission, verify:**
- [ ] All Phase 0 security items complete + approved by security review (if applicable)
- [ ] FCM push works end-to-end on real device (not emulator)
- [ ] Boot receiver tested: reboot phone, reminder fires at scheduled time
- [ ] Privacy/Terms reviewed by legal (if applicable)
- [ ] Crashlytics live: throw test exception, verify it surfaces in console
- [ ] Play Integrity checks pass (if strict mode enabled)
- [ ] Release build (R8 on) size <150MB
- [ ] All Roborazzi screenshot tests pass
- [ ] Manual smoke test: signup → create event type → send invite link → book → receive confirmation → set reminder → cancel booking

**Before AppFunctions public beta:**
- [x] ~~Manifest hosted + discoverable~~ — superseded: on-device discovery via AndroidX AppFunctions (KSP schema + manifest registration); HTTP manifest superseded by the MCP/CLI workstream (decision #3)
- [ ] All 6 shipped functions verified on an API 36 emulator (decision #1)
- [ ] Copilot integration tested manually (at least one function call end-to-end)
- [ ] Rate limits enforced; no user can DoS another user (per-IP tiers live; per-user limits open — decision #2)
- [x] Scope isolation verified: user A cannot read/write user B's data (server-side owner scoping + `requireSignedIn` gate; demo sessions rejected)
- [ ] **MCP/CLI agent surface decision #3 executed**: MCP endpoint and/or Termux CLI shipped (scope in the Phase 3 decisions block)

