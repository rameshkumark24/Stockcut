# StockCut Build Checklist

**The executable checklist for this app.** Instantiated from the generic Vibe Coding checklist, with everything irrelevant to an offline Android utility removed and everything Play-specific added.

**How to use it:** work top to bottom. Each phase gates the next. Tick items in a PR, not in your head — the git diff is the record of what actually got done.

**Where the generic checklist still applies:** as a template for your *next* app. Do not execute from it here.

---

## ✅ Phase 0 — Scope & Feasibility *(complete)*

See [`00-phase-0-scope-feasibility.md`](00-phase-0-scope-feasibility.md) and [`00-gap-audit.md`](00-gap-audit.md).

- [x] Problem in one sentence
- [x] Single core job defined
- [x] Target user identified
- [x] Competitor scan done — market is **not** empty, wedge identified
- [x] Success metrics picked (30-day return ≥ 30%, free→paid ≥ 2%)
- [x] MVP line drawn, "later" list written down
- [x] Stack locked with reasons
- [x] Cost model — **₹0/month at any scale**
- [x] Spend ceiling — ₹2,500 total, once
- [x] Assumptions and risks listed
- [ ] ⚠️ **App name + package name cleared on Play Store** — *package name is permanent*

## ✅ Phase 1 — Requirements & Documents *(complete)*

- [x] PRD · TRD · App flow · UI/UX brief · Data model · Test plan · Implementation plan
- [x] Optimizer module contract *(replaces "API contract" — no API)*
- [x] Entitlement matrix *(replaces "auth & roles matrix" — no accounts)*
- [x] Build-variant config plan *(replaces "dev/staging/prod")*
- [x] `CLAUDE.md` at repo root
- [x] Decision recorded: no analytics SDK in v1
- [x] `git init` + `.gitignore` **before** the first AI prompt

---

## 🔴 Phase W0 — Pre-code gates *(you are here)*

**Nothing below this line starts until both gates clear.**

- [ ] 🔴 **Name one real tradesman** — phone number, agreed to look at things
- [ ] 🔴 **Paper test** — take one of their real jobs, produce the cut plan by hand, show them. They say "I'd use that", or the project stops.
- [ ] Play Console account created ($25 paid)
- [ ] **Tester recruitment started** — spreadsheet of Gmail addresses, target 15, ≥ 3 real tradesmen
- [ ] Privacy policy written and live on a public URL (GitHub Pages)
- [x] ~~Redirect page for the feedback form~~ — **not needed**, the form was dropped 2026-08-07

---

## Phase 2 — Design

- [ ] Wireframe all 7 screens before styling anything
- [ ] Colour tokens implemented as a Compose theme, light + dark
- [ ] Type scale implemented, **measurements use tabular figures**
- [ ] Spacing scale (4/8/12/16/24/32/48) — nothing else
- [ ] All 10 components in the inventory sketched
- [ ] **All four states drawn for every screen** — loading, empty, error, success
- [ ] First-run experience: seeded example job, not an onboarding carousel
- [ ] Contrast verified with a tool at ≥ 4.5:1 — **not by eye** (used in sunlight)
- [ ] Tap targets ≥ 48 dp, primary buttons 56 dp
- [ ] Microcopy written (UI/UX brief §8) — before build, not during
- [ ] Destructive actions: undo snackbar for rows, dialog for projects

## ✅ Phase 3 — Core modules *(complete)* *(replaces "Architecture & Data Model")*

**Build these before a single screen exists.**

- [x] Gradle multi-module skeleton: `:data :optimizer :units` — *`:app` deferred to Phase 5*
- [x] Version catalog, `targetSdk = 36`, `minSdk = 26`
- [x] `:units` — parse + format, all 5 unit systems
- [x] `:units` — fractional inch, feet notation (`8' 3 1/2"`)
- [x] `:units` — round-trip property test passing
- [x] `:units` — exactness table verified (1/64" == 127 U)
- [x] `:optimizer` — data classes exactly per the contract
- [x] `:optimizer` — Best-Fit-Decreasing + bounded improvement pass
- [x] `:optimizer` — self-verifies the invariant before returning
- [x] `:optimizer` — property tests: invariant, conservation, non-negativity, determinism (≥ 1000 cases each) — *1,500–20,000 seeded cases each*
- [x] `:optimizer` — oracle set `O-01`…`O-09` green — [ ] `O-10` **blocked on the W0 tradesman gate**
- [x] `:optimizer` — benchmarks within budget (20 / 200 / 1000 parts)
- [x] **Zero `android.*` imports in `:optimizer` and `:units`** — verified in CI
- [x] **No `Float`/`Double`** outside `wastePercent` — verified in CI
- [x] GitHub Actions runs both suites on every push
- [ ] Waste-% baseline committed for the **whole** oracle set *(only the example job is pinned today, at 13.75%)*

## ✅ Phase 4 — Data layer *(complete)*

- [x] Room entities, DAOs, database (4 tables — 3 per job, plus `stock_profile`)
- [x] Constraints at the **DB level**: NOT NULL, CHECK > 0, ON DELETE CASCADE — *CHECK via `RAISE(ABORT)` triggers, see `docs/05` §1.3*
- [x] Indexes on both `project_id` foreign keys
- [x] `created_at` / `updated_at` on `project`, stored **UTC**
- [x] `exportSchema = true`, v1 JSON **committed to git**
- [x] Migration test infrastructure set up
- [x] **`fallbackToDestructiveMigration()` absent** — verified by grep in CI
- [x] DataStore settings + entitlement cache
- [ ] Android auto-backup enabled — **blocked: needs the `:app` manifest (Phase 5)**
- [x] Example project seeded on first run *(seed data + tests; the write happens in Phase 5)*

> ⚠️ **CI compiles the instrumented tests but never runs them** — cascade deletes, CHECK
> constraints and migrations are only verified when someone runs
> `./gradlew :data:connectedDebugAndroidTest` against a device. Do it before any schema change.

## ✅ Phase 5 — UI build *(complete)*

- [x] Navigation graph, 5 routes
- [x] **`MeasurementField`** — accepts `1200`, `3/4`, `1 5/16`, `8' 3 1/2"`
- [x] S1 Projects list — all states
- [x] S2 Project editor — Parts / Stock / Setup tabs
- [x] Unit change re-formats display, **never mutates stored values** (tested, and verified on a device)
- [x] S3 optimize transition + validation
- [x] 🔴 **Infeasible parts block navigation** — verified on a device with a 7000 mm part against 6000 mm stock
- [x] S4 Cut plan + **`CutPlanBar`**
- [x] Identical-bar collapsing (`×4 identical bars`)
- [x] Share as image via `FileProvider` — **no storage permission** (both manifests declare zero)
- [x] PDF export, legible in black and white — every segment outlined, not just filled
- [x] S6 Settings · S7 About
- [x] No spinner on operations under 300 ms

> ⚠️ **Restore purchases** is absent — it needs Play Billing (Phase 6). It is
> mandatory for reinstalls and reviewers look for it (gap audit §B4), so it
> cannot be skipped, only deferred.
>
> There is deliberately **no "Report a problem" link**: the app collects nothing
> from its users. See [`09-feedback-channel.md`](09-feedback-channel.md).

## Phase 6 — Monetisation

- [ ] Play Console in-app product created — one-time non-consumable, $4.99
- [ ] Billing Library **7.0.0+**
- [ ] Purchase **acknowledged immediately** (unacknowledged > 3 days = auto-refund)
- [ ] Entitlement cached in DataStore, **authoritative offline**
- [ ] 🔴 **Never downgrade a paid user on a failed offline check**
- [ ] **Restore purchases** in Settings, About, and the paywall
- [ ] Free-tier gates: 20 parts / 1 project / 5 stock / no PDF
- [ ] Existing data stays editable at the limit — never lock someone out of their own work
- [ ] AdMob banner + interstitial (after every 3rd optimize, never mid-task)
- [ ] **UMP consent flow**, verified against an EU IP
- [ ] 🔴 **Test ad unit IDs in every non-production variant** — verified in the built artifact
- [ ] Ad container collapses when a load fails — no blank grey box
- [ ] In-app review prompt: after a *successful* optimize, ≥ 3 lifetime, ≤ 1 per 90 days

## ~~Phase 7 — Feedback channel~~ *(not applicable — removed 2026-08-07)*

🔴 **The app collects nothing from its users.** No feedback form, no bug-report
form, no survey, no in-app submission. Every item that was in this phase is void.

See [`09-feedback-channel.md`](09-feedback-channel.md) for the decision and its
reasoning. The short version: the compliance argument for the form only held
while the form kept one exact shape, and the first one built in practice arrived
carrying Name and Email fields. A channel whose correctness depends on nobody
editing a form is a liability, and removing it costs one permission and gains a
data safety declaration with nothing on it but AdMob and Crashlytics.

- [x] Support contact: a `mailto:` link in About — **not** a collection channel;
      it opens the user's own mail app and sends nothing on its own

## Phase 8 — Testing & QA

- [ ] Full `:optimizer` + `:units` suites green, ≥ 1000 property cases
- [ ] Waste-% baseline recorded; regressions fail the build
- [ ] Room migration tests green
- [ ] ViewModel tests
- [ ] 8 critical-path Compose UI tests
- [ ] Billing matrix fully walked with a license tester
- [ ] Edge cases: empty, 1000 parts, unicode/emoji labels, SQL chars, double-tap Optimize, double-tap Buy
- [ ] **Low-end real device** (2–3 GB RAM, Android 8–10)
- [ ] **Max system font scale** — every screen, nothing clipped
- [ ] Dark mode — cut-plan segments still distinguishable
- [ ] Rotation, process death, split screen
- [ ] **Airplane mode — full core function**
- [ ] Sunlight test — read a cut plan outdoors
- [ ] 🔴 **Uninstall → reinstall → projects restored** via auto-backup
- [ ] Cold start < 1.5 s, AAB < 12 MB

## Phase 9 — Store readiness & compliance

- [ ] `targetSdk = 36` confirmed **in the built AAB**, not just the gradle file
- [ ] 🔴 **Keystore generated, Play App Signing enrolled**
- [ ] 🔴 **Keystore + passwords backed up in TWO places off this machine** — losing it means the app can never be updated
- [ ] `versionCode` monotonic, `versionName` semver
- [ ] Secrets scanned across **git history**, not just current files
- [ ] `keystore.properties`, `*.jks`, `local.properties` confirmed absent from history
- [ ] **Data safety form** — AdMob + Crashlytics only. **No user-submitted data at all**, so nothing else to declare
- [ ] **Content rating** questionnaire
- [ ] **Ads declaration** — "contains ads"
- [ ] Privacy policy URL live and linked in the listing
- [ ] Store title with primary keyword
- [ ] **80-char short description** — highest-weight ASO field
- [ ] Long description
- [ ] **5 screenshots, every one captioned**
- [ ] Feature graphic 1024×500
- [ ] Licence check on every dependency
- [ ] Play payout: bank + PAN configured; India tax treatment confirmed with a CA

## Phase 10 — Closed test 🔴

- [ ] Internal testing track first — catch the obvious in 24 h
- [ ] AAB uploaded to closed testing
- [ ] 15 tester emails added
- [ ] Opt-in instructions sent personally
- [ ] 🔴 **≥ 12 testers confirmed opted in** — the 14-day clock starts only then
- [ ] Clock held **14 continuous days** — a drop-out breaks it
- [ ] Crash + ANR checked daily in Vitals
- [ ] Feedback collected, especially from the tradesmen
- [ ] Crash-free ≥ 99.5%, no P0 open
- [ ] Production-access answers drafted with specifics, not boilerplate

## Phase 11 — Launch

- [ ] Production access granted
- [ ] **Staged rollout at 20%**, not 100%
- [ ] Smoke test on a real device from the live listing
- [ ] Vitals watched hourly for 48 h
- [ ] Ramp to 100% only if clean
- [ ] Rollback plan: halt rollout in Play Console

## Phase 12 — Post-launch

- [ ] Reply to **every** review for the first 6 months
- [ ] Feedback form triaged weekly
- [ ] Watch 30-day return rate against the ≥ 30% target
- [ ] Watch free→paid against the ≥ 2% target
- [ ] Rating held ≥ 4.3
- [ ] Dependency update cadence: monthly
- [ ] Tech-debt log started
- [ ] **Aug 31, 2027** — next target API level deadline. Diarise it now.

---

## Parallel tracks

### Tester recruitment *(W0 → Phase 10)*
The longest lead-time item and the **only thing that can hard-block launch**.
- [ ] 15 recruited · [ ] ≥ 3 tradesmen · [ ] all opted in · [ ] 14 days held

### Legal & compliance *(W0 → Phase 9)*
- [ ] Privacy policy live · [ ] covers AdMob and Crashlytics
- [ ] Data safety form matches reality
- [ ] Account deletion: **N/A — no accounts** (record the reason, don't leave it blank)
- [ ] Refund policy: Google's standard applies
- [ ] Age rating stated

---

## Deliberately deleted from the generic checklist

Recorded so it's clear these were **considered and excluded**, not forgotten.

| Deleted | Why |
|---|---|
| Auth: hashing, JWT, sessions, 2FA, password reset, email verification | No accounts |
| Authorization: RLS, IDOR tests, admin guards, mass assignment | No server, no multi-user data |
| CORS, CSRF, rate limiting, webhook signatures, SQL injection | No API surface |
| DB perf: N+1, EXPLAIN, cursor pagination, connection pooling, 100k seed | Local SQLite, tens of rows |
| Server observability: correlation IDs, `/health`, p95 latency, uptime | No server. Replaced by **Android Vitals** |
| Backups, PITR, RTO/RPO, staging env | No server data. Replaced by **auto-backup + migration tests** |
| CDN, cache headers, bundle splitting, Core Web Vitals | Not a web app |
| Custom domain, SSL, SPF/DKIM/DMARC, 404/500 pages | No website |
| Idempotency keys | Google Play handles purchase idempotency |
| Feature flags / kill switch | No server to flip them from. Rollback = halt the staged rollout |
| Cost alerts, spend ceiling monitoring | **₹0/month.** Nothing to alert on |
| DPDP verifiable parental consent | Not a minors' app, no personal data from users |
| Content moderation, crisis escalation | No user-generated content |
