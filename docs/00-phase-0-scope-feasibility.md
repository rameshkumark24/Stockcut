# Phase 0 — Scope & Feasibility

**Project:** working name *StockCut* — 1D linear cut-list optimizer for Android
**Date:** 2026-08-04 · **Owner:** Rameshkumar · **First Play Store app**
**Gate:** all 11 Phase 0 boxes from the [Vibe Coding checklist](https://app.notion.com/p/3942caadb481802b8251c114e82dd296) answered below. Two items are ⚠️ OPEN and block Phase 1.

---

## 1. The problem, in one sentence

> A fabricator, framer, or joiner buys material in fixed stock lengths, works out the cutting order in their head or on a scrap of paper, and throws away **10–25% of it as offcuts** — on a job with £2,000 of steel tube that is £200–500 gone, every job, forever.

**Who hurts:** metal fabricators, welders, timber framers, shopfitters, fence and deck builders, glaziers, anyone cutting from **linear stock** — bar, tube, angle, extrusion, pipe, rebar, studs, trim.
**How much:** material is typically the largest line item on a small fabrication job. A 10-point waste improvement is pure margin.

## 2. The single core job

> **Turn "here are the stock lengths I have" + "here are the pieces I need" into a cutting plan with the least waste — accounting for the saw blade width — in under 60 seconds, on a phone, with no internet, in the units the user actually works in.**

Not in the job: 2D panel nesting (v2), cost estimating, inventory, quoting, CAD, project management.

## 3. Target user + one real person

**Primary:** self-employed or small-shop tradesman, **US / UK / Canada / Australia**, age 30–55, works in **imperial fractional inches** (US) or **mm** (UK/AU), phone in a pocket on a dusty job site, spotty signal, will pay $5 for a tool that saves a length of steel once.

**Why not India as the primary market:** India Android ad eCPM is ~$1.85 rewarded / $1.88 interstitial vs **~$16.49 / $14.32 in the US** — 8–15× less for identical effort. Build the app in India, sell it to Tier 1. The app is language-neutral; only unit conventions differ.

⚠️ **OPEN — name one real tradesman you can show this to.** A welder, carpenter, fabricator, fencing contractor — anyone who cuts from stock lengths. You need one person to (a) confirm the pain is real, (b) tell you which units they use, (c) sanity-check that a cut plan you produce by hand is actually usable. Without this you are guessing at a trade you don't work in. *This blocks Phase 1.*

## 4. What already exists, and where the gap actually is

**Correction to my earlier read:** I called this "thin competition." That was wrong for **2D panel cutting**, which is crowded and largely free.

| Competitor | Shape | Cost | Gap it leaves |
|---|---|---|---|
| [SmartCut: Cut List Optimizer](https://play.google.com/store/apps/details?id=tr.com.yazilimk.smartcutpro) (Play) | 2D guillotine panels, kerf + trim | Freemium | Panel-first; linear is secondary |
| [CutList Optimizer](https://apps.apple.com/us/app/cutlist-optimizer/id6465744401) (iOS) | 2D, "AI solver" | Paid | **iOS only** |
| Cut Optimizer (iOS) | 2D, kerf, grain, edge banding, PDF/SVG | Paid | **iOS only** |
| [OptiCutter](https://www.opticutter.com/cut-list-optimizer) | Browser, 2D + linear | **Free** tier | Needs browser + internet + typing a table on a phone |
| [Cutlist Evolution](https://cutlistevo.com/) | Browser, sheet + linear + roll | **Free** | Same |
| [Cutlistor](https://www.cutlistor.com/free-cut-list-optimizer), CutListCalc | Browser, panel-first | **Free** | Same |

**Where the real gap is — four wedges, all of them defensible:**

1. **1D linear, not 2D panels.** Everyone optimises plywood for cabinetmakers. Almost nobody builds a *phone-native* tool for the guy cutting 6m steel tube or 16ft studs. Metal trades are underserved.
2. **Offline and phone-native.** The free web tools all assume a browser, a keyboard, and signal. Your user is standing next to a chop saw in a shed.
3. **Fractional inches done properly.** Most tools force decimals. A US carpenter types `1 5/16"`, not `1.3125`. This alone is a review-driver.
4. **Speed of entry.** Saved stock profiles ("my usual: 6m × 50×50 SHS"), quantity multipliers, one-thumb entry. Existing tools make you fill a spreadsheet.

**Honest read:** you are not entering an empty market. You are entering a market where every incumbent is either on the wrong platform (iOS), the wrong shape (browser), or aimed at the wrong trade (cabinetmakers). That is a real opening — but it means **execution on those four wedges is the whole business.** A generic clone loses.

## 5. Success metrics — two only

1. **30-day return rate ≥ 30%** — of users who complete one optimization, how many come back within 30 days for a second job? A tool used once is a novelty; a tool used per-job is a business.
2. **Free → paid conversion ≥ 2%** — does the unlock actually convert?

**Supporting signal (not a target):** Play Store rating ≥ 4.3 with ≥ 25 ratings by month 3. Below 4.0, ASO collapses and nothing else matters.

**Deliberately not tracked:** total downloads, DAU, session length. This is a tool — a *short* session that solves the problem is the win.

## 6. MVP line

### v1 — ship exactly this

- **Stock input:** length + quantity (or "unlimited"), multiple stock sizes per project
- **Parts input:** length × quantity, optional label
- **Kerf** setting (saw blade width) — non-negotiable, this is what makes it a real tool
- **Units:** mm · cm · m · decimal inch · **fractional inch** (1/8, 1/16, 1/32 denominators)
- **Optimize →** per-stock-bar cut plan: cut sequence, offcut remaining, total waste %, bars needed
- **Save/load projects** locally
- **Export:** share as image + PDF
- **Free tier:** up to 20 parts, 1 saved project, banner + occasional interstitial
- **Paid unlock $4.99 one-time:** unlimited parts, unlimited projects, PDF export, no ads
- **Restore purchases** button

### Later list — written down so it stops nagging

2D panel/guillotine cutting · grain direction · edge banding · CSV/spreadsheet import · cost per length and total job cost · multiple materials in one project · label printing · cloud sync · team sharing · iOS · web version · dark-mode-only theming work · subscription tier · offcut inventory ("I have these leftovers, use them first" — *strong v2 candidate, tradesmen love this*).

## 7. Tech stack, and why each piece

| Layer | Choice | Why |
|---|---|---|
| App | **Kotlin + Jetpack Compose**, native Android | Smallest APK, best cold start on the low-end phones your users carry, Play Billing and AdMob are first-class citizens. No cross-platform bridge to debug. Android-only is the plan, so cross-platform buys nothing. |
| *Fallback* | Flutter | Only if you're already fluent in Dart and not in Kotlin. Do **not** use React Native/Expo here — larger app, fiddlier IAP, and EAS costs money you don't need to spend. |
| Persistence | **Room (SQLite)** | Projects, stock profiles, settings. Migrations tested (gap audit §B9). |
| Optimizer | **Pure Kotlin module, zero Android dependencies** | So it can be unit-tested headlessly and fast. First-Fit-Decreasing + local improvement pass. Integer tenths-of-mm internally. |
| Billing | **Play Billing Library 7.0.0+** | Required for new submissions. One-time non-consumable. Entitlement cached locally. |
| Ads | **AdMob + UMP SDK** | UMP is mandatory for the EU/UK traffic you're targeting. Test ad units in all dev builds. |
| Crash reporting | **Firebase Crashlytics** (free) | Declare it in the data safety form. |
| Analytics | **None in v1** | Play Console gives you installs, ratings, and Vitals free. Adding an analytics SDK adds a privacy declaration for data you won't act on yet. |
| Backend | **None.** No network permission except ads. | This is the entire cost strategy. |
| CI | GitHub Actions → build AAB, run optimizer tests | Tests gate the build, per checklist Phase 10. |
| `targetSdk` | **36** | Mandatory for new apps from Aug 31, 2026 (gap audit §B1). |
| `minSdk` | **26** (Android 8.0) | Covers old workshop phones without legacy API pain. |

Locked here so the agent doesn't re-litigate the stack every session — this file feeds `CLAUDE.md`.

## 8. Cost model — 10 / 1,000 / 10,000 users

| Line | 10 users | 1,000 users | 10,000 users |
|---|---|---|---|
| Servers | **$0** | **$0** | **$0** |
| Database | **$0** | **$0** | **$0** |
| Storage / egress | **$0** | **$0** | **$0** |
| Crashlytics | $0 | $0 | $0 |
| Privacy policy hosting (GitHub Pages) | $0 | $0 | $0 |
| Play Console | **$25 once** | — | — |
| **Monthly cost** | **$0** | **$0** | **$0** |

> **Zero marginal cost is the entire point of this project.** Your last idea cost ₹38,000–60,000/month at 10,000 users. This one costs ₹0. Ten thousand users and one user cost exactly the same.

**Revenue side, for calibration** — 10,000 lifetime downloads at 2% conversion:
200 × $4.99 = ~$998 gross → **~$848 after Play's 15% fee** (verify current fee tier in Console), plus modest ad revenue from the free tier. That is lifetime, not monthly.

## 9. Hard spend ceiling

**₹2,500 total. Once.**

- Play Console registration: $25 (~₹2,100)
- Optional domain for the privacy policy: skip it — GitHub Pages is free
- **If any recurring cost appears, something is wrong with the architecture.** There is no service to autoscale, no bill to alert on.

The scarce resource on this project is your **time**, not money. Budget that instead: **6 weeks build + 3 weeks closed test.**

## 10. Name + domain availability — ⚠️ OPEN

**"CutList" is heavily used** — collides with SmartCut, CutList Optimizer, Cutlistor, CutListCalc, Cutlist Evolution. Do not use it as your primary title.

Before Phase 1:

```bash
# Play Store + App Store name collision — search manually
# Play: "stock cut" / "cut optimizer" / "cutting plan"
# Domain (only needed if you want a landing page; GitHub Pages is enough for the privacy policy)
whois stockcut.app; whois cutplan.app
```

**ASO tension to resolve deliberately:** the title needs the search keyword (`cut list`, `cutting`, `optimizer`) but must be distinctive. A pattern that satisfies both: **`StockCut — Cut List Optimizer`** — distinctive brand + keyword tail. Decide before you write the store listing, because it also sets the package name, which is permanent.

⚠️ **Owner action: pick 3 candidates, check all three on Play, decide.** *This blocks Phase 1.*

## 11. Assumptions and risks

| # | Assumption / risk | Severity | How it fails | Mitigation / test |
|---|---|---|---|---|
| A1 | Tradesmen will trust a cut plan from an unknown app | 🔴 | Nobody uses it twice | Show a hand-verified plan to the §3 real person before writing code |
| A2 | The optimizer is correct | 🔴 | Wasted material → 1-star reviews that never expire | Property test on the length invariant, known-optimal oracle set, waste-% regression suite (gap audit §B6) |
| A3 | Fractional inches implemented correctly | 🔴 | US market rejects it | Fraction round-trip tests; US tester in the closed test |
| A4 | ASO alone can deliver installs | 🟠 | Zero downloads, zero revenue | Keyword-led title, screenshots showing a finished plan, reply to every review |
| A5 | 12 testers can be found | 🟠 | Cannot reach production at all | Recruit **15**, start collecting emails this week |
| A6 | API 36 done right | 🟠 | Submission rejected / app not discoverable | `targetSdk = 36` from commit #1 |
| A7 | Keystore survives | 🔴 | **App can never be updated again** | Play App Signing + two off-machine backups (gap audit §B3) |
| A8 | AdMob policy not violated | 🔴 | Account termination, earnings forfeited | Test ad units only in dev; never click your own live ads |
| A9 | Free web tools don't eat the market | 🟠 | No reason to install | Win on offline + phone-native + 1D + fractional inches, or don't ship |
| A10 | $4.99 is the right price | 🟡 | Low conversion | Ship at $4.99; A/B is not available to you at this scale, so pick and hold for 3 months |
| A11 | Solo maintenance is sustainable | 🟡 | Abandoned app with paying users | Zero backend means the app keeps working untouched — this risk is genuinely low here |
| A12 | Play payout / India tax handled | 🟡 | Money stuck or tax surprise | Confirm PAN + bank setup in Console; ask a CA about export-of-service treatment before revenue arrives |

## 12. Go / no-go

**GO.** This clears every filter the last idea failed: zero marginal cost, no personal data, no moderation, no minors, no regulatory wall, no content to write, and a testable definition of "correct."

Ordered next actions:

1. **Close §3** — name one real tradesman. *(Blocks everything.)*
2. **Close §10** — pick and clear the name. It sets the permanent package name.
3. **Zero-code test:** take a real job from that person — their stock lengths and their required pieces — and produce the optimal cut plan **by hand on paper**. Show it to them. If they shrug, stop here. Cost: ₹0.
4. **Start recruiting 15 testers now**, in parallel with everything else. It's the longest lead-time item.
5. **Pay the $25** and create the Play Console account — the account age itself is not a gate, but the sooner it exists the sooner you can configure things.
6. Only then: Phase 1 docs — PRD, TRD, app flow, UI/UX brief, local schema, implementation plan, test plan, and `CLAUDE.md` (stack, conventions, "optimizer module has zero Android dependencies", "integer tenths-of-mm internally", "targetSdk 36").
7. Build the **optimizer module first, headless, fully tested**, before a single screen exists. It is the product; the UI is packaging.

---

*Companion document: [`00-gap-audit.md`](00-gap-audit.md) — what the Vibe Coding checklist deletes and what it misses for an offline Android utility.*
