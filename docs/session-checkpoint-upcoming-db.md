# Objective

- Triage open PR #19 (event-type mutations), trace and fix all CodeRabbit review findings, then restructure the project directory (src/ + scripts/ layout) as a follow-up PR.

## Important Details

- Repo: /Users/derrickhodge/Downloads/upcoming-db (github.com/DSamuelHodge/upcoming-db); Cloudflare Workers API (Hono 4.13.5 + Drizzle + LibSQL/Turso), live at [https://upcoming-db-api.dshodge2020.workers.dev](https://upcoming-db-api.dshodge2020.workers.dev); deploys are manual (wrangler deploy), no deploy CI.

- PR #19 "Event-type create/update/delete endpoints (owner-scoped)" — 3 CodeRabbit findings, all confirmed valid:

- CR-1 (Security, worker.ts:318-321): GET /event-types default exposed other owners' inactive event types to JWT callers.

- CR-2 (Functional, POST worker.ts:392-397 / PATCH :458-463): c.req.json() SyntaxError misreported as "locations is not valid JSON" (root cause: locationsColumn transform at :336-338 throws bare SyntaxError from JSON.parse inside Zod .transform()).

- CR-3 (Functional, :395/:461 duplicated builder): empty-path .refine issue (empty PATCH body) produced "invalid input: " — message dropped.

- Auth model (worker.ts:124-142): JWT sets authUserId, shared secret sets authIsAdmin; mutually exclusive (middleware returns after JWT success).

- User decisions (all "Recommended"): minimal src/ + scripts/ layout (no domain layering, no worker.ts split); tests colocated with source; restructure lands as separate PR after #19 merges; full docs pass ([AGENTS.md](http://AGENTS.md) + Docs path refs).

- Repo conventions: merge commits (gh pr merge --merge), commit prefix style fix: review — ....

- schema.sql must stay colocated with schema-sql.ts (reads new URL("./schema.sql", import.meta.url), schema-sql.ts:8).

- /me endpoints (worker.ts:629, :695) also have the path-only builder but were deliberately NOT touched (out of review scope, minimal diff).

- Docs/[api-contract.md](http://api-contract.md) predates PR #19 — the 3 new mutation endpoints are missing from it (noted as optional follow-up; user didn't request it).

- Environment: macOS 12.6.0, node 22; npm ci was required (stale node_modules lacked hono).

## Work State

### Completed

- Phase A — all 3 CodeRabbit fixes implemented on phase-9-event-type-mutations, committed as bcb6102 "fix: review — owner-scoped inactive listing, body-vs-locations JSON errors, issue messages in 400 detail", pushed; CI green (16s).

- CR-1: scope = activeOnly ? eq(isActive,true) : jwtUserId === undefined ? undefined : or(eq(isActive,true), eq(ownerUserId,jwtUserId)) (or was already imported).

- CR-2: two-phase parse in POST + PATCH — body parse failure → 400 "body must be valid JSON"; SyntaxError escaping schema.parse keeps "locations is not valid JSON".

- CR-3: zodDetail(err) helper — ${path}: ${message}, bare message when path empty; used in both handlers.

- Tests added/strengthened in worker.test.ts: empty-PATCH asserts "invalid input: at least one field to update is required"; malformed body POST+PATCH → "body must be valid JSON"; malformed locations string → "locations is not valid JSON"; JWT listing-scoping assertions (active cross-owner, inactive owner-only, ?activeOnly=true, admin sees all).

- npm ci run (fixed missing hono); verify: typecheck clean, 79 tests — 65 pass / 14 live-instance skips / 0 fail.

- CodeRabbit did not re-review; user said proceed without it. PR #19 MERGED (2026-08-30T14:30:02Z, merge commit 9382ee3da92a27751ef1c5b3d88cdd58e7434684).

- Phase B on new branch chore/src-scripts-layout (from updated main):

- git mv done: 24 files → src/ (worker.ts, worker.test.ts, schema.ts, schema.sql, schema-sql.ts, json-columns.ts, event-types.ts, availability-engine.ts/.test, multi-host-routing.ts/.test, create-booking-handler.ts/.test, auth.ts, crypto.ts/.test, user-metadata.ts, daily.ts/.test, notifications.ts, logger.ts, test-db.ts, libsql-instance.test.ts); 7 files → scripts/ (apply-schema.ts, apply-additive-2026-08-28.ts, apply-additive-2026-08-29-auth.ts, apply-additive-2026-08-30-single-use-links.ts, check-schema-drift.ts, seed-bookings.ts, seed-live.ts).

- Import fixes: scripts/apply-schema.ts → "../src/schema-sql"; scripts/check-schema-drift.ts → "../src/schema" (other scripts had no relative imports).

- package.json: test: "tsx --test \"src/**/*.test.ts\""; schema:apply/schema:additive-2026-08-28/drift:check → tsx scripts/....

- tsconfig.json: include: ["src/**/*.ts", "scripts/**/*.ts", "drizzle.config.ts"]; wrangler.toml: main = "src/worker.ts"; drizzle.config.ts: schema: "./src/schema.ts".

- Verified post-move: typecheck clean; npm test finds all 79 tests via the glob (65/14/0); npx -y wrangler@latest deploy --dry-run --outdir /tmp/opencode/wrangler-dry succeeds (855.03 KiB; macOS version warning is harmless).

### Active

- Docs pass on chore/src-scripts-layout: scoping grep just completed. Reference counts: Docs/[architecture-analysis.md](http://architecture-analysis.md) (create-booking-handler.ts ×17, test-db.ts ×8, schema.ts ×8, availability-engine.ts ×7, daily.ts ×5, notifications.ts ×3, libsql-instance.test.ts ×3, apply-schema.ts ×2, multi-host-routing.ts ×1, event-types.ts ×1); Docs/[remediation-plan.md](http://remediation-plan.md) (schema.ts ×4, create-booking-handler.ts ×3, notifications.ts ×2, create-booking-handler.test.ts ×2, availability-engine.test.ts ×2, test-db.ts ×1, daily.ts ×1, apply-schema.ts ×1); Docs/[api-contract.md](http://api-contract.md) (schema.ts ×1, multi-host-routing.ts ×1, create-booking-handler.ts ×1); [AGENTS.md](http://AGENTS.md) (libsql-instance.test.ts ×3, schema.ts ×2, notifications.ts ×2, daily.ts ×2, create-booking-handler.ts ×2, apply-schema.ts ×2, test-db.ts ×1, schema-sql.ts ×1, multi-host-routing.ts ×1, daily.test.ts ×1, availability-engine.ts ×1, availability-engine.test.ts ×1). Note: worker.ts appears 0 times in Docs ([AGENTS.md](http://AGENTS.md) describes it by name though).

### Blocked

- (none)

## Next Move

1. Run word-boundary path-prefix pass over Docs/*.md + [AGENTS.md](http://AGENTS.md) (perl/sed s/\bNAME\.ts\b/src\/NAME.ts/ for runtime files, scripts/ for the 7 script files, plus schema.sql → src/schema.sql; run once, longest/most-specific names safe — schema.ts cannot match inside schema-sql.ts/schema.sql with .ts boundary).

2. Hand-edit [AGENTS.md](http://AGENTS.md): replace "flat single-package repo (all .ts at root; no src/)" with src/ + scripts/ layout description; update commands section (script paths, test is now a glob over src/**/*.test.ts — the "four test files listed explicitly" note is stale); update schema/env/architecture-section file paths.

3. Commit (style: chore: ... — e.g. "chore: src/ + scripts/ layout; keep runtime colocated, scripts separate"), push chore/src-scripts-layout, open PR with body summarizing moves + touchpoints; confirm CI green.

4. Report to user: #19 merged (note: live worker runs pre-fix code until manual npx wrangler deploy — deploy after restructure merges since wrangler.toml main changes in this PR); ask whether to also merge the restructure PR (merging it was not explicitly approved).

## Relevant Files

- worker.ts (now src/worker.ts): 1192 lines; POST/PATCH /event-types and GET /event-types fixes; auth middleware at :124-142; locationsColumn transform :336-338; zodDetail after UpdateEventTypeInput.

- worker.test.ts (now src/worker.test.ts): phase-9 test block ~:893+; seed() at :15; authed() at :49; appWithAuth() at :562 (API_SECRET + JWT_SECRET); jwtHeaders pattern ~:1036-1058.

- package.json, tsconfig.json, wrangler.toml, drizzle.config.ts: all restructure edits applied.

- scripts/apply-schema.ts, scripts/check-schema-drift.ts: imports updated to ../src/*.

- src/schema-sql.ts + src/schema.sql: must stay colocated (import.meta.url read).

- [AGENTS.md](http://AGENTS.md), Docs/[remediation-plan.md](http://remediation-plan.md), Docs/[architecture-analysis.md](http://architecture-analysis.md), Docs/[api-contract.md](http://api-contract.md): pending path-reference pass (counts above).

- .github/workflows/ci.yml + drift.yml: npm-commands only, no path references — no changes needed.

- src/daily.ts: cwd-relative readFileSync(".env"); src/test-db.ts: writes booking-test-*.db relative to cwd — both unaffected (npm scripts run from root).