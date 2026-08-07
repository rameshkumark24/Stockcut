# Everything remaining until the app is live on Play

**As of 2026-08-07.** Phases 0–5 are complete. This is what is left, who owns
each item, and what blocks what.

Two columns matter more than the rest: **🧑 you** (accounts, money, paperwork,
real devices) and **🤖 me** (code). Items marked ⏱ are calendar time and cannot
be compressed by working harder.

---

## Where things actually stand

| | Status |
|---|---|
| Phase 0 Scope · Phase 1 Docs | ✅ complete |
| Phase 3 `:units` + `:optimizer` | ✅ complete |
| Phase 4 `:data` | ✅ complete |
| Phase 5 UI — all 7 screens, share, PDF | ✅ complete |
| Phase 6 Billing + ads | ❌ **not started** |
| Phase 7 Feedback channel | ⬛ removed — app collects nothing |
| Phase 8 Testing & QA | 🟡 partial |
| Phase 9 Store readiness | ❌ not started |
| Phase 10 Closed test | ❌ not started ⏱ |
| Phase 11 Launch | ❌ not started |

**167 tests green.** CI green on `main`. Release build works under R8 and comes
out at **1.7 MB** — comfortably inside the 12 MB NFR-3 budget.

---

## 🔴 The three things that gate everything else

Nothing downstream can start until these do. They are all yours, and two of them
are slow.

1. **Play Console account — $25.** Blocks the in-app product, license testers,
   the closed test, and therefore Phase 6 testing, Phase 9, 10 and 11. Identity
   verification takes days on top of the payment.
2. ⏱ **The 14-day closed test.** 12 testers opted in *continuously*. Cannot start
   until an account exists and a build is uploaded and approved. This is three
   weeks of calendar time minimum, including the production-access review.
3. 🔴 **The keystore.** Generate it, back it up twice off-machine, enrol in Play
   App Signing. Losing it means the app can never be updated again.

---

# 🧑 Yours — accounts, money, paperwork

## Immediate (minutes, unblocks me)

- [ ] **Merge PR #3** — brings the privacy policy to the root of `main`
- [ ] **Enable GitHub Pages** — Settings → Pages → `main` + **`/(root)`**, then
      confirm `https://rameshkumark24.github.io/Stockcut/privacy-policy.html`
      loads. Required for the store listing.

## Play Console (blocks Phase 6 testing)

- [ ] Create account, pay **$25**, complete identity verification
- [ ] Create the app entry as `com.measure.stockcut`
- [ ] Upload the first billing build to **Internal testing**
      *(needed before Monetise unlocks — I produce the build, you upload)*
- [ ] Create the in-app product: **one-time, non-consumable, $4.99** →
      **send me the product ID**
- [ ] Add **license testers** (Settings → License testing, `RESPOND_NORMALLY`)
- [ ] Payments profile: bank + tax details
- [ ] 🔴 India: confirm export-of-service treatment of foreign app revenue
      **with a CA**

## Keystore

- [ ] Generate `upload-keystore.jks` — command in
      [`11-play-and-admob-setup.md`](11-play-and-admob-setup.md) Part 4
- [ ] Fill in `keystore.properties` from the example
- [ ] 🔴 Back up the file **and** its passwords in **two places off this machine**
- [ ] Enrol in **Play App Signing** at first upload

## Store listing — ASO is your entire distribution channel

- [ ] Title: **StockCut — Cut List Optimizer**
- [ ] **80-character short description** — the highest-weight ASO field
- [ ] Long description
- [ ] **5 screenshots, each captioned** — content specified in `docs/04` §11
- [ ] Feature graphic **1024×500**
- [ ] Content rating questionnaire
- [ ] **Ads declaration** — "contains ads"
- [ ] **Data safety form** — AdMob + Crashlytics only. Nothing user-submitted
      exists to declare.

*I can draft the title, both descriptions and the screenshot captions. The images
themselves need a device.*

## Real-device testing — I cannot do these

The emulator cannot answer any of these honestly.

- [ ] 🔴 **Low-end real device** (2–3 GB RAM, Android 8–10) — *this is your actual
      user's phone*
- [ ] 🔴 **Uninstall → reinstall → jobs restored** via auto-backup. The rules are
      written but have never been proven to work.
- [ ] Cold start **< 1.5 s** on that device *(release build — the 4.5 s I measured
      was a debug build on an emulator and is not comparable)*
- [ ] **Airplane mode** — full core function
- [ ] Sunlight — read a cut plan outdoors
- [ ] Process death — background 20 min, return to the same state
- [ ] Split screen

## Testers ⏱

- [x] 15 collected
- [ ] All 15 **opt in via the link and install from Play** — invited-but-not-
      installed does not count
- [ ] ≥ 3 are real tradesmen *(release gate, `docs/06` §10)*
- [ ] Hold **12 opted in for 14 continuous days**
- [ ] Apply for production access — answer with specifics, not boilerplate

---

# 🤖 Mine — code

## Phase 6 — Monetisation *(the big one)*

**Billing**
- [ ] Play Billing Library **7.0.0+**
- [ ] Purchase flow, **acknowledged immediately** — unacknowledged > 3 days is
      auto-refunded by Google, so this is a money bug, not polish
- [ ] Entitlement cached in DataStore, **authoritative offline**
- [ ] 🔴 **Never downgrade a paid user on a failed offline check**
- [ ] **Restore purchases** in Settings, About and the paywall
- [ ] Real `PaywallSheet` — replaces the three `AlertDialog` placeholders now
      standing in on S1, S2 and S4
- [x] Free-tier gates *(20 parts / 1 project / 5 stock / no PDF — already built
      and tested)*

**Ads**
- [ ] AdMob SDK + banner + interstitial *(IDs already wired, test-safe)*
- [ ] 🔴 **UMP consent flow**, re-enterable from Settings
- [ ] Interstitial after every 3rd optimize, **never mid-task**
- [ ] Ad container **collapses** when a load fails — no blank grey box
- [ ] `INTERNET` + `ACCESS_NETWORK_STATE` + `BILLING` permissions land here, with
      the SDKs that need them

**Other**
- [ ] In-app review prompt — after a *successful* optimize, ≥ 3 lifetime,
      ≤ 1 per 90 days *(the rules are already written and tested; only the Play
      API call is missing)*
- [ ] Crashlytics wired *(`google-services.json` is in place)*

## Phase 8 — Testing gaps

- [ ] **7 of 8 critical-path Compose tests.** Only #4 (fractional inch) is
      automated. Missing: first-run → example → plan; new job → optimize;
      21st part → paywall; infeasible blocks navigation; delete → undo;
      share chooser; rotate result.
      *Five of these I have verified by hand on a device — but by hand is not a
      regression test.*
- [ ] ViewModel tests (~15 wanted)
- [ ] Waste-% baseline for the **whole** oracle set — only the example job is
      pinned today, at 13.75%
- [ ] Edge cases not yet exercised: 1000 parts end-to-end, double-tap Optimize,
      double-tap Buy
- [ ] 🔴 Oracle case **`O-10`** — *blocked on a real tradesman job*. Send me one
      and it becomes a permanent regression test.

## Phase 9 — Release engineering

- [ ] **Release signing config** in `build.gradle.kts`, reading
      `keystore.properties` *(needs your keystore first — the release APK builds
      today but comes out unsigned)*
- [ ] `versionCode` / `versionName` bump policy
- [ ] Secrets scanned across **git history**, not just current files
- [ ] Licence check on every dependency
- [ ] Confirm `targetSdk = 36` **in the built AAB**, not just the gradle file

---

## Rough sequence

```
NOW      merge PR #3 · enable Pages                          🧑 minutes
         Phase 6 billing + ads code                          🤖 days
         ↓
         Play Console account + $25                          🧑 days (verification)
         ↓
         upload build to Internal testing                    🧑
         create in-app product + license testers             🧑
         ↓
         walk the billing matrix                             🤖🧑 needs both
         keystore + backups                                  🧑
         store listing + screenshots                         🧑🤖
         low-end device + auto-backup testing                🧑
         ↓
         CLOSED TEST — 12 testers, 14 days                   ⏱ 2 weeks
         ↓
         production access application                       ⏱ up to 7 days review
         ↓
         staged rollout at 20% → watch Vitals 48 h → 100%
```

**Realistic floor: about 4 weeks**, and only if the Play Console account is
created in the next day or two. The 14-day test plus a review of up to 7 days is
three of those weeks and no amount of effort shortens it.

The single highest-value thing you can do today is **create the Play Console
account**, because the closed-test clock cannot start until it exists.
