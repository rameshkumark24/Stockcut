# Gap Audit — Vibe Coding checklist vs. an offline Android utility app

**App:** 1D linear cut-list optimizer. Offline, on-device, Android-only, paid unlock + ads.
**Audited against:** [Notion — Vibe Coding master build checklist](https://app.notion.com/p/3942caadb481802b8251c114e82dd296) (Aug 2026 revision).
**Date:** 2026-08-04.

---

## Verdict in one line

The checklist is written for a **web app with a server**. This app has no server. That deletes roughly **60% of the checklist** — and everything it deletes is replaced by a different set of items it doesn't contain at all: **Android release engineering, Play Console compliance, billing, ads policy, algorithm correctness, and ASO.** Those six are where a first Play Store app actually fails.

---

## Part A — What you can delete (the subtraction)

Every line below is on the checklist and **does not apply**. Cross them off deliberately, don't just skip them — knowing *why* they're gone is what stops you re-adding them.

| Checklist area | Status | Why |
|---|---|---|
| **Phase 3 — Architecture & Data Model** | ~80% N/A | No ERD, no foreign keys, no multi-tenancy, no soft-delete strategy, no seed script, no object storage, no background jobs. You have one local SQLite table set for saved projects. |
| **Phase 5 — Authentication** | **100% N/A** | No accounts. No passwords, no bcrypt, no JWT, no session revocation, no password reset, no 2FA, no email verification, no account enumeration. |
| **Phase 5 — Authorization / IDOR** | **100% N/A** | No RLS, no ownership checks, no admin routes, no mass assignment. There is one user and it's whoever holds the phone. |
| **Phase 5 — Input & endpoints** | ~90% N/A | No SQL injection surface, no CSRF, no CORS, no rate limiting, no webhook signatures, no file uploads. |
| **Phase 6 — Database performance** | **100% N/A** | No N+1, no EXPLAIN ANALYZE, no cursor pagination, no connection pooling, no 100k-row seed. |
| **Phase 6 — Frontend/API perf** | ~70% N/A | No CDN, no cache headers, no bundle splitting, no Core Web Vitals. Replaced by APK size + cold-start time + jank. |
| **Phase 8 — Observability** | ~70% N/A | No structured server logs, no correlation IDs, no `/health` endpoint, no p95 latency, no uptime monitoring. Replaced by **Android Vitals** (§B10). |
| **Phase 9 — Reliability** | ~80% N/A | No server backups, no PITR, no RTO/RPO, no staging environment mirroring prod, no graceful third-party degradation. Replaced by **on-device data durability** (§B9). |
| **Phase 10 — Deployment** | ~50% N/A | No custom domain, no SSL, no www redirect, no SPF/DKIM/DMARC, no 404/500 pages, no zero-downtime deploy. Replaced by the **Play release track pipeline** (§B1–B3). |
| **Legal — DPDP / GDPR heavy items** | Mostly N/A | You collect no personal data yourself. **But AdMob does** — see §B5. This is the one place the deletion is partial and dangerous. |

**What survives from the original checklist, unchanged and still mandatory:** Phase 0 (all of it), Phase 1 docs (PRD, TRD, app flow, UI/UX brief, implementation plan, `CLAUDE.md`), Phase 2 UI/UX in full, Phase 4 vibe-coding discipline (git before first prompt, commit before every AI session, verify every package, `.env` in `.gitignore`, read every generated file), Phase 7 testing, and the cost/spend-ceiling items.

---

## Part B — What the checklist is missing (the addition)

### B1. 🔴 Target API 36 — hard deadline in 27 days

**From August 31, 2026, all new apps submitted to Google Play must target Android 16 (API 36).** Existing apps must be on at least API 35 to stay discoverable to new users on Android 16+ devices. An extension is available to **Nov 1, 2026**.

You are starting on **Aug 4, 2026** and will realistically submit in **October** (build + 14-day closed test). So:

- [ ] **Set `targetSdk = 36` on day one.** Do not build against 35 and "upgrade later" — API 36 has behaviour changes (edge-to-edge enforcement, background restrictions) that are cheap to design for and expensive to retrofit.
- [ ] Set `minSdk` deliberately — recommend **API 26 (Android 8.0)** to cover old workshop phones without dragging legacy APIs.

### B2. 🔴 The 12-tester gate — it's calendar time, not work

Personal Play Console accounts created after Nov 13, 2023 cannot publish to production until a **closed test runs with ≥12 testers opted in for 14 continuous days**, followed by a production-access application you must answer questions on.

- [ ] Recruit **15 testers, not 12** (people drop out, and the count must hold continuously)
- [ ] Collect their Gmail addresses into a Play Console email list **before** the build is ready
- [ ] Start the clock the day the closed track goes live — this is 3+ weeks of your timeline
- [ ] **Never buy testers.** Paid tester services get accounts terminated; you lose the $25 and the account permanently
- [ ] Have testers actually *open and use* the app — Google asks how you tested

### B3. 🔴 Keystore custody — the one unrecoverable mistake

Not on the checklist anywhere. Lose your upload keystore and you **cannot ever update your app**. Not "hard to recover" — cannot.

- [ ] Enrol in **Play App Signing** (Google holds the app signing key; you hold only the upload key)
- [ ] Back up `upload-keystore.jks` + its passwords in **two places off your machine** (password manager + encrypted cloud)
- [ ] `*.jks` and `keystore.properties` in `.gitignore` **before the first commit** — same rule the checklist has for `.env`
- [ ] Ship **AAB**, not APK

### B4. 🔴 Google Play Billing — the money path

- [ ] **Play Billing Library 7.0.0+** (required for new submissions)
- [ ] Product type: **one-time non-consumable** unlock — not a subscription
- [ ] **Entitlement cached locally** so the paid user stays paid offline. This is the #1 refund-and-1-star generator in offline apps.
- [ ] **"Restore purchases" button** — mandatory for reinstalls and new devices, and reviewers look for it
- [ ] License testers configured in Play Console so you can test purchase flows without spending real money
- [ ] Play's service fee: **15% on the first $1M/year** — verify current terms in Console before pricing
- [ ] Payout setup: bank account + tax details (PAN) in Play Console — verify India-specific tax/GST treatment of foreign app revenue with a CA

### B5. 🔴 AdMob — the one place privacy law re-enters

You collect nothing. **AdMob does.** This is where the deleted Legal items come back.

- [ ] **UMP SDK / Google consent management** implemented — required for EU/UK users, and you are deliberately targeting Tier 1 traffic
- [ ] **Privacy policy URL** — required by Play whenever any SDK collects data. Host it free on GitHub Pages.
- [ ] **Data safety form** completed honestly (AdMob collects device/advertising ID → you must declare it)
- [ ] **Ads declaration** in Play Console set to "contains ads"
- [ ] **`app-ads.txt`** published if you have a website
- [ ] **Use test ad units during all development.** Clicking your own live ads = AdMob account termination and forfeited earnings.
- [ ] Ad placement policy: no ads adjacent to buttons, no ads during the optimize action, no full-screen ad on app open before first interaction

### B6. 🔴 Algorithm correctness *is* the product

The checklist has one line: "Unit tests on business logic and calculations." For this app, that line is the entire quality bar. A wrong cut plan costs a tradesman real money and earns a 1-star review that never goes away.

- [ ] **Test oracle strategy**: a set of inputs with known-optimal answers, checked into the repo
- [ ] **Property tests**: total of all cut pieces + (kerf × number of cuts) + offcut = stock length, for every bar, always. This invariant catches almost every real bug.
- [ ] **Regression suite on waste %**: if a code change makes any known case worse, the build fails
- [ ] **Never produce an infeasible plan**: a required piece longer than any available stock must be reported clearly, not silently dropped
- [ ] Floating-point discipline — work in integer tenths-of-a-mm internally, format at display time only (same principle as the checklist's "store timestamps in UTC, convert at display")

### B7. 🔴 Units — where trade apps actually die

Not on the checklist at all, and it will decide your US ratings.

- [ ] Support **fractional inches** (`3/4"`, `1 5/16"`), not just decimals. American woodworkers and fabricators do not think in `0.8125`.
- [ ] Support mm / cm / m / decimal inch / fractional inch, with a per-project unit setting
- [ ] Round-trip safe: enter fractional, store integer, display fractional, no drift
- [ ] Kerf entered in the same unit system as the project
- [ ] Fraction denominator setting (1/8, 1/16, 1/32)

### B8. 🟠 ASO — this is your entire distribution

The checklist has "SEO basics: meta tags, OG image, sitemap." The mobile equivalent is missing, and unlike a website you have **no other channel**.

- [ ] Keyword research on Play Store search terms before you name the app
- [ ] App title carrying the primary keyword
- [ ] Short description (80 chars) — this is the highest-weight ASO field
- [ ] Long description written for humans first, keywords second
- [ ] **Screenshots that show a finished cut plan**, with a caption on each — most installs are decided here
- [ ] Feature graphic (1024×500)
- [ ] Localised store listing for at least US English; consider adding a metric-market listing later
- [ ] Plan for first 25 ratings — the ASO cold start

### B9. 🟠 On-device data durability

Replaces the server-backup phase. Losing a tradesman's saved job list is a 1-star.

- [ ] Room/SQLite **migrations written and tested** (an unmigrated schema change wipes user data)
- [ ] Android **auto-backup** enabled and verified with a real uninstall/reinstall cycle
- [ ] Manual **export** (JSON or CSV) as a user-visible escape hatch
- [ ] Test the "phone full / write fails" path

### B10. 🟠 Android Vitals replaces observability

Google **demotes your store ranking** if you exceed bad-behaviour thresholds. This is not optional monitoring — it's a distribution penalty.

- [ ] Crash rate and **ANR rate** watched in Play Console from the first closed-test day
- [ ] Cold start time measured on a low-end device
- [ ] Crash reporting wired (Crashlytics or Sentry free tier) — and declared in the data safety form
- [ ] Excessive wakeup / battery metrics clean (you're offline, so this should be trivially fine)

### B11. 🟠 Device reality testing

The checklist says "real device testing — iOS Safari and Android Chrome." Wrong shape. Replace with:

- [ ] One **low-end real device** (2–3GB RAM, Android 8–10) — this is your actual user's phone
- [ ] Small screen + large screen
- [ ] **System font scale at maximum** — tradesmen are often 45+ and use large text. Layouts break here.
- [ ] Dark mode
- [ ] Rotation / split screen
- [ ] Gloves-and-sunlight scenario: high contrast, large tap targets, readable outdoors

### B12. 🟡 Output must leave the phone

The cut plan is useless trapped in the app. Not a checklist item, but it is a core requirement.

- [ ] Share as image (WhatsApp to the apprentice)
- [ ] Export PDF (print at the shop)
- [ ] Legible when printed in black and white

### B13. 🟡 Reviews are your support channel

Phase 11 says "support channel live and monitored." For a Play app that is literally the reviews tab.

- [ ] Reply to every review in the first 6 months
- [ ] In-app "email support" link — deflects 1-stars into emails you can actually fix
- [ ] In-app rating prompt via the **Play In-App Review API**, triggered after a *successful* optimize, never on launch

---

## Part C — Gaps in the *idea*, not the checklist

Answered in `00-phase-0-scope-feasibility.md`:

1. **Competition is real.** SmartCut Pro on Play, two iOS apps, and four free browser tools. My earlier "thin competition" read was wrong for 2D panel cutting. The Phase 0 doc moves the wedge to 1D linear.
2. **Free alternatives.** OptiCutter, Cutlistor, Cutlist Evolution, CutListCalc are all free in a browser. You must be better *on a phone, offline, on a job site* — that is the only defensible ground.
3. **Zero audience.** No email list, no following, no website. ASO is the only channel.

---

## Part D — Recommended amendments to the Notion checklist

Add a **"Mobile / Play Store" variant** of the checklist with these swaps:

| Replace | With |
|---|---|
| Phase 3 Architecture & Data Model | **Local persistence & migrations** (Room, auto-backup, export) |
| Phase 5 Security (auth + authz) | **Play compliance & billing integrity** (keystore, entitlement caching, restore purchases, ads policy) |
| Phase 6 DB performance | **App performance** (APK size, cold start, jank, low-end device) |
| Phase 8 Observability | **Android Vitals** (crash rate, ANR rate, store-ranking thresholds) |
| Phase 9 Reliability | **On-device durability** (backup/restore, migration safety) |
| Phase 10 SEO basics | **ASO** (title, short description, screenshots, feature graphic) |
| — (new) | **Release gates**: target API level deadline, 12-tester closed test, production access application |

Keep unchanged: Phase 0, Phase 1 docs, Phase 2 UI/UX, Phase 4 vibe-coding discipline, Phase 7 testing, cost ceiling.

---

## Sources

- [Target API level requirements for Google Play apps (Play Console Help)](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Meet Google Play's target API level requirement (Android Developers)](https://developer.android.com/google/play/requirements/target-sdk)
- [Google Play API 36 deadline: Aug 31, 2026 (Vadimages)](https://vadimages.com/news/google-play-api-36-deadline-august-2026-logistics-apps)
- [App testing requirements for new personal developer accounts (Play Console Help)](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Google Play closed testing requirements for new personal developer accounts, 2026 (Aerious)](https://aerious.uk/blog/google-play-closed-testing-requirements-for-new-personal-developer-accounts-2026)
- [SmartCut: Cut List Optimizer (Google Play)](https://play.google.com/store/apps/details?id=tr.com.yazilimk.smartcutpro&hl=en_US)
- [CutList Optimizer (App Store)](https://apps.apple.com/us/app/cutlist-optimizer/id6465744401)
- [OptiCutter — Cutlist Optimizer](https://www.opticutter.com/cut-list-optimizer)
- [Cutlist Evolution — free browser optimizer](https://cutlistevo.com/)
- [The 2026 AdMob & Mobile Monetization Playbook (MonetizeMore)](https://www.monetizemore.com/blog/admob-monetization/)
