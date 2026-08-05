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
- [ ] **Google Form for feedback created** — see [`09-feedback-channel.md`](09-feedback-channel.md)

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

## Phase 3 — Core modules *(replaces "Architecture & Data Model")*

**Build these before a single screen exists.**

- [ ] Gradle multi-module skeleton: `:app :data :optimizer :units`
- [ ] Version catalog, `targetSdk = 36`, `minSdk = 26`
- [ ] `:units` — parse + format, all 5 unit systems
- [ ] `:units` — fractional inch, feet notation (`8' 3 1/2"`)
- [ ] `:units` — round-trip property test passing
- [ ] `:units` — exactness table verified (1/64" == 127 U)
- [ ] `:optimizer` — data classes exactly per the contract
- [ ] `:optimizer` — Best-Fit-Decreasing + bounded improvement pass
- [ ] `:optimizer` — self-verifies the invariant before returning
- [ ] `:optimizer` — property tests: invariant, conservation, non-negativity, determinism (≥ 1000 cases each)
- [ ] `:optimizer` — oracle set `O-01`…`O-10` green
- [ ] `:optimizer` — benchmarks within budget (20 / 200 / 1000 parts)
- [ ] **Zero `android.*` imports in `:optimizer` and `:units`** — verified
- [ ] **No `Float`/`Double`** outside `wastePercent`
- [ ] GitHub Actions runs both suites on every push

## Phase 4 — Data layer

- [ ] Room entities, DAOs, database (3 tables)
- [ ] Constraints at the **DB level**: NOT NULL, CHECK > 0, ON DELETE CASCADE
- [ ] Indexes on both `project_id` foreign keys
- [ ] `created_at` / `updated_at` on `project`, stored **UTC**
- [ ] `exportSchema = true`, v1 JSON **committed to git**
- [ ] Migration test infrastructure set up
- [ ] **`fallbackToDestructiveMigration()` absent** — verified by grep
- [ ] DataStore settings + entitlement cache
- [ ] Android auto-backup enabled
- [ ] Example project seeded on first run

## Phase 5 — UI build

- [ ] Navigation graph, 5 routes
- [ ] **`MeasurementField`** — accepts `1200`, `3/4`, `1 5/16`, `8' 3 1/2"`
- [ ] S1 Projects list — all states
- [ ] S2 Project editor — Parts / Stock / Setup tabs
- [ ] Unit change re-formats display, **never mutates stored values** (tested)
- [ ] S3 optimize transition + validation
- [ ] 🔴 **Infeasible parts block navigation** — never show a plan that silently dropped a part
- [ ] S4 Cut plan + **`CutPlanBar`**
- [ ] Identical-bar collapsing (`×4 identical bars`)
- [ ] Share as image via `FileProvider` — **no storage permission**
- [ ] PDF export, legible in black and white
- [ ] S6 Settings · S7 About
- [ ] No spinner on operations under 300 ms

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

## Phase 7 — Feedback channel *(new — see [`09-feedback-channel.md`](09-feedback-channel.md))*

- [ ] Google Form created — 4 fields only
- [ ] 🔴 **No email, name, or contact field** — the form is anonymous by design
- [ ] Field 2 prompt worded to get reproducible detail (you cannot ask a follow-up)
- [ ] Email notification on new response enabled
- [ ] Pre-filled link generated, `entry.*` IDs extracted into `local.properties`
- [ ] Diagnostics string built at runtime and prefilled — **visible and editable by the user**
- [ ] Diagnostics contains no advertising ID, install ID, location, or project contents
- [ ] Opens via `Intent.ACTION_VIEW` — **no WebView**
- [ ] Separate `mailto:` support link in About, for users who want a reply
- [ ] `mailto:` fallback when offline or no browser; copy-to-clipboard if neither
- [ ] Entry points: About + "This plan looks wrong" on the cut plan screen
- [ ] Never gated behind the paywall, never nagged, never after a crash
- [ ] Data safety form covers it *(no new category — Crashlytics already declares Diagnostics)*
- [ ] Privacy policy paragraph added, with a retention period
- [ ] Triage routine agreed (weekly, 15 min)

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
- [ ] **Data safety form** — AdMob + Crashlytics + feedback form, declared honestly
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
- [ ] Privacy policy live · [ ] covers AdMob, Crashlytics, feedback form
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
