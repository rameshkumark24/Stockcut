# Implementation Plan — StockCut

**Start:** 2026-08-04 · **Target production submission:** week of 2026-10-05
**Constraint:** two of the nine weeks are the Play closed test, which is calendar time you cannot compress.

---

## Critical path

```
W0  ── name + tradesman + paper test ── Play Console account created
W1  ── :units + :optimizer (headless, no UI)
W2  ── :data + MeasurementField
W3  ── project editor screens
W4  ── result screen + share/PDF
W5  ── billing + ads + paywall
W6  ── polish, device matrix, store assets
W7  ── internal test → CLOSED TEST STARTS ─┐
W8  ── closed test running (14 days)       │ 12 testers, continuous
W9  ── production access application → submit ─┘
```

**Longest lead-time item is tester recruitment.** Start it in W0, not W7.

---

## W0 — Before any code *(this week)*

Nothing here is optional and none of it is code.

| Task | Exit criteria |
|---|---|
| Name one real tradesman | You have their phone number and they've agreed to look at things |
| **Paper test** | Take one of their real jobs, produce the cut plan by hand, show them. They say "yes, I'd use that" or you stop. |
| Choose app name + package name | Checked on Play Store, no collision. **Package name is permanent.** |
| Create Play Console account | $25 paid, account live |
| **Start recruiting 15 testers** | A spreadsheet of Gmail addresses. Target ≥ 3 real tradesmen. |
| Write the privacy policy | Live on GitHub Pages, public URL |
| `git init` + first commit | Before the first AI prompt, per checklist Phase 4 |
| `.gitignore` | `keystore.properties`, `*.jks`, `local.properties`, `google-services.json` — **before the first commit** |
| `CLAUDE.md` in repo root | Copied from [`CLAUDE.md`](CLAUDE.md) |

> 🔴 **Gate: if the paper test fails, stop the project here.** Cost so far: one afternoon.

---

## W1 — `:units` + `:optimizer`

**No UI this week. No Android. Pure Kotlin.** This is deliberate: the product is the algorithm, and it is fully testable without an emulator.

| Task | Notes |
|---|---|
| Gradle multi-module skeleton | `:app :data :optimizer :units`, version catalog, `targetSdk 36`, `minSdk 26` |
| `:units` — parse + format | All 5 unit systems, fractional inch, feet notation |
| `:units` — property tests | Round-trip, exactness table, rejection cases |
| `:optimizer` — data classes | Exactly the contract in `05-data-model-and-optimizer-contract.md` §2.1 |
| `:optimizer` — Best-Fit-Decreasing | Plus the bounded improvement pass |
| `:optimizer` — self-verification | Invariant checked before returning; never return an unbalanced plan |
| `:optimizer` — property tests | Invariant, conservation, non-negativity, determinism — ≥ 1000 cases each |
| Oracle set `O-01`…`O-09` | Committed, all green |
| Benchmarks | 20 / 200 / 1000 parts within budget |
| GitHub Actions | Runs both test suites on every push |

**Exit criteria:** `./gradlew :optimizer:test :units:test` green in under 5 seconds, ≥ 1000 property cases, oracle set passing, benchmarks within budget. **Zero Android imports in either module.**

> This is the week that decides whether the app is any good. Do not rush it into W2.

---

## W2 — Persistence + the measurement field

| Task | Notes |
|---|---|
| Room entities, DAOs, database | Three tables per the data model doc |
| Migration test infrastructure | Set it up now, before you need it |
| `exportSchema = true`, v1 JSON committed | |
| DataStore settings + entitlement cache | |
| Repositories | Room ↔ domain mapping |
| **`MeasurementField` component** | The hard one. Numeric-first keyboard with `/` and `'`, live formatting on blur, inline errors. |
| Seed the example project | "Example: gate frame" on first run |

**Exit criteria:** you can create a project in a test, save parts, reopen, and `MeasurementField` accepts `1 5/16` and renders it back correctly.

---

## W3 — Project editor

| Task | Notes |
|---|---|
| Navigation graph | 5 routes |
| S1 Projects list | Populated + empty + free-tier-limit states |
| S2a Parts tab | Add / edit / delete + undo snackbar |
| S2b Stock tab | Quick-add chips, unlimited toggle |
| S2c Setup tab | Units, denominator, kerf, trim, name |
| ViewModels + `StateFlow` | |
| Unit-change re-formatting | Values must not mutate — covered by test |

**Exit criteria:** full round trip — create a job, enter stock and parts in fractional inches, close the app, reopen, everything intact.

---

## W4 — The payoff screen

| Task | Notes |
|---|---|
| S3 optimize transition + validation | Including the infeasible banner that **blocks navigation** |
| S4 result screen | Summary strip, waste badge, bar list |
| **`CutPlanBar` component** | The signature visual. Proportional segments, labels, grey offcut. |
| Identical-bar collapsing | `×4 identical bars` |
| Share as image | `FileProvider` into cache dir — **no storage permission** |
| PDF export | Platform `PdfDocument`; legible in black and white |
| Shortfall banner | |

**Exit criteria:** a real job from your tradesman contact produces a plan they confirm is correct and usable. Screenshot it — this is store asset #1.

---

## W5 — Money

| Task | Notes |
|---|---|
| Play Console: create the in-app product | One-time non-consumable, $4.99 |
| Billing Library 7+ integration | Purchase, acknowledge **immediately**, query on launch |
| Entitlement cache | Offline-authoritative; never downgrade on a failed offline check |
| **Restore purchases** | Settings + About + paywall |
| S5 Paywall sheet | Contextual headline per trigger |
| Free-tier gates | 20 parts / 1 project / 5 stock / no PDF |
| AdMob banner + interstitial | Test ad IDs only |
| **UMP consent flow** | Verified against an EU IP |
| Ad cadence | Interstitial after every 3rd optimize, never mid-task |
| In-app review prompt | After successful optimize, ≥ 3 lifetime, ≤ 1 per 90 days |

**Exit criteria:** the entire billing matrix in test plan §5 walked with a license tester, including uninstall → reinstall → restore.

---

## W6 — Polish and store assets

| Task | Notes |
|---|---|
| Device matrix walk | Low-end device, max font scale, dark mode, rotation, airplane mode |
| Cold start + APK size against NFRs | < 1.5 s, < 12 MB |
| Crashlytics wired | |
| Accessibility pass | Content descriptions, tap targets ≥ 48 dp, contrast verified with a tool |
| **Store listing** | Title with keyword, 80-char short description, long description |
| **5 screenshots + feature graphic** | Per UI/UX brief §11, every one captioned |
| Data safety form | AdMob + Crashlytics declared honestly |
| Content rating questionnaire | |
| Ads declaration | "Contains ads" |
| Keystore generated + **backed up twice off-machine** | |
| Play App Signing enrolled | |

**Exit criteria:** release-signed AAB builds, installs on the low-end device, everything in test plan §9 satisfied.

---

## W7 — Closed test starts 🔴

| Task | Notes |
|---|---|
| Internal testing track first | Catch the obvious in 24 h |
| Upload AAB to **closed testing** | |
| Add all 15 tester emails | |
| Send instructions personally | Opt-in link + what to try. A tester who doesn't opt in doesn't count. |
| **Confirm ≥ 12 opted in** | The 14-day clock starts only once 12 are in |
| Day-1 Vitals check | |

> 🔴 **The 14-day clock requires 12 testers opted in *continuously*.** If someone opts out on day 9, the count breaks. This is why you recruited 15.

---

## W8 — Closed test running

| Task | Notes |
|---|---|
| Daily crash + ANR check | |
| Collect feedback, especially from the tradesmen | |
| Ship fixes to the closed track | Updates are fine and don't reset the clock; **losing testers does** |
| Finalise store listing copy | |
| Prepare production-access answers | Google asks how you tested and what you changed |

**Exit criteria:** 14 continuous days elapsed with ≥ 12 opted-in testers · crash-free ≥ 99.5% · no P0 bugs open.

---

## W9 — Submit

| Task | Notes |
|---|---|
| Apply for production access | Answer the questionnaire specifically — "12 testers used it on real jobs for 14 days; 3 are working fabricators; here's what they found and what I changed" |
| Await approval | Days, not hours |
| Production release, **staged rollout at 20%** | Per checklist Phase 10's soft-launch rule |
| Watch Vitals hourly for 48 h | |
| Ramp to 100% if clean | |

---

## Milestones and gates

| # | Milestone | Week | Gate |
|---|---|---|---|
| M0 | Idea validated on paper | W0 | 🔴 **Tradesman says yes, or stop** |
| M1 | Optimizer correct | W1 | Property tests + oracle set green |
| M2 | Data round-trips | W2 | Save/reopen preserves everything |
| M3 | Editor usable | W3 | Full job entry works |
| M4 | Plan produced | W4 | 🔴 **Tradesman confirms a real plan is correct** |
| M5 | Monetisation works | W5 | Billing matrix walked |
| M6 | Release candidate | W6 | Release gate (test plan §10) green |
| M7 | Closed test complete | W8 | 14 days × 12 testers, crash-free ≥ 99.5% |
| M8 | Live | W9 | Production access granted |

**M0 and M4 are the two stop-or-continue gates.** Both are a real tradesman looking at a real plan.

---

## Dependencies and risks to the schedule

| Risk | Impact | Mitigation |
|---|---|---|
| Fewer than 12 testers opt in | **Cannot launch at all** | Recruit 15 from W0; personal follow-up |
| A tester drops mid-window | Clock breaks | Buffer of 3 |
| Production access rejected | +1–2 weeks | Answer the questionnaire with specifics, not boilerplate |
| Optimizer harder than expected | +1 week | It's isolated and headless — it slips alone, without blocking UI work |
| `MeasurementField` harder than expected | +3 days | Budgeted a full week in W2; it deserves it |
| Aug 31 API-36 deadline | Miss = not discoverable | `targetSdk 36` from commit #1; extension to Nov 1 exists as a backstop |
| Life / exams / job | Slips everything | 9 weeks is calendar, not effort. Protect W1 and W7 above all else. |

---

## Parallel tracks

| Track | Runs during | Why |
|---|---|---|
| **Tester recruitment** | W0 → W7 | Longest lead time; the only thing that can hard-block launch |
| **Store listing copy + ASO keyword research** | W4 → W6 | Your only distribution channel |
| **Privacy policy + data safety** | W0, finalised W6 | Required before submission |
| **Tradesman feedback loop** | W0, W4, W7 | Three touchpoints, not one |

---

## What to build first, restated

**Build the optimizer, headless and fully tested, before a single screen exists.**

It is the product. The UI is packaging. If you build screens first, you will discover the kerf arithmetic is wrong in week 5, with three screens depending on it.
