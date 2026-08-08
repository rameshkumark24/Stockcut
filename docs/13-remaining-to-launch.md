# Everything remaining until the app is live on Play

**Rewritten 2026-08-08.** Two settled decisions shape this list:

1. **StockCut is completely free**, earning only from AdMob — see
   [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).
2. **It publishes on the owner's own Play Console account**, opened when the $25
   is available.

**🧑 you** · **🤖 me** · **⏱ calendar time that effort cannot compress**

---

## 🔴 Read this before planning anything

**The 12-tester / 14-day closed test applies to you.**

Google requires it of every **personal** developer account created after
13 November 2023. A new account is a new account: you run a closed test with at
least **12 testers opted in continuously for 14 days**, then *apply* for
production access, and that application is reviewed for up to 7 days.

Nothing in this repository can shorten that. It is roughly **three weeks of
calendar time after the day you pay**, and it cannot start until the account
exists and a build is uploaded.

The practical consequence: **pay the $25 as early as you can**, because the clock
starts at the account, not at the code. The code has been ready since today.

---

## Where things stand

| | Status |
|---|---|
| Phase 0 Scope · Phase 1 Docs | ✅ |
| Phase 3 `:units` + `:optimizer` | ✅ |
| Phase 4 `:data` | ✅ |
| Phase 5 UI — all 7 screens, share, PDF | ✅ |
| Phase 6 Ads + consent + review prompt | ✅ |
| Phase 6 Billing | ⬛ written, switched off — the app sells nothing |
| Phase 7 Feedback channel | ⬛ removed — the app collects nothing |
| Phase 8 Testing & QA | ✅ except the real-device items |
| Phase 9 Store readiness | ✅ **assets produced, ready to paste** |
| Phase 10 Closed test | ⏱ blocked on the account |
| Phase 11 Launch | blocked on Phase 10 |

**195 tests green** (144 JVM, 51 instrumented). Signed release AAB **v1.0.0**,
9.2 MB, verified inside the bundle: `com.measure.stockcut`, targetSdk 36,
minSdk 26.

---

## What is already produced and waiting

Nothing here needs code. It is all sitting in the repo.

| Thing | Where |
|---|---|
| Signed release bundle | `app/build/outputs/bundle/release/app-release.aab` |
| Upload keystore | `upload-keystore.jks` *(git-ignored)* |
| Keystore passwords | `keystore.properties` *(git-ignored)* |
| Listing copy, length-checked | [`16-store-listing.md`](16-store-listing.md) |
| 5 screenshots | `store/screenshots/` |
| Feature graphic 1024×500 | `store/feature-graphic.png` |
| `app-ads.txt` | `store/app-ads.txt` |
| Upload runbook | [`17-upload-day-runbook.md`](17-upload-day-runbook.md) |

---

# 🧑 Yours

## Now — free, and does not wait for the $25

- [ ] 🔴 **Back up `upload-keystore.jks` and its passwords in two places off this
      machine.** It is generated and working; it is not backed up. Losing it means
      the app can never be updated again — not patched, not renamed
- [ ] Create a GitHub repo named exactly **`rameshkumark24.github.io`** and publish
      `store/app-ads.txt` at its root. It must resolve at
      `https://rameshkumark24.github.io/app-ads.txt` — the `/Stockcut/` project
      page will not do, because `app-ads.txt` has to sit at the **domain root**
- [ ] Line up your **15 testers** and confirm they will install from a Play link
      when asked. Invited-but-not-installed does not count toward the 12
- [ ] Burn the captions into the 5 screenshots
- [ ] Export the 512×512 icon from `app/src/main/res/mipmap-xxxhdpi`

## 🔴 The real-device testing — I cannot do any of these

The emulator cannot answer them honestly, and it is better to find these now than
during the 14-day test.

- [ ] **Low-end real phone** (2–3 GB RAM, Android 8–10) — *your actual user's phone*
- [ ] **Uninstall → reinstall → jobs come back** via auto-backup. The rules are
      written and have never been proven to work
- [ ] Cold start **< 1.5 s** on that phone, release build
- [ ] **Airplane mode** — full function, and the ad slot collapses rather than
      leaving a grey box
- [ ] Read a cut plan outdoors in sunlight
- [ ] Background it 20 minutes, return to the same state
- [ ] Split screen

## When the $25 arrives

- [ ] Create the Play Console account and complete **identity verification**
      *(days, not minutes — this is the first thing on the critical path)*
- [ ] Create the app: **StockCut — Cut List Optimizer**, `com.measure.stockcut`,
      App, Free
- [ ] Upload `app-release.aab`, accept **Play App Signing**
- [ ] Fill the listing from [`16-store-listing.md`](16-store-listing.md)
- [ ] Declarations: **Ads = Yes**, **In-app purchases = No**, content rating,
      and 🔴 **data safety must declare AD_ID**
- [ ] Set **Developer website** to `https://rameshkumark24.github.io` so AdMob can
      verify the app against a domain you control

## ⏱ Then the part that is pure waiting

- [ ] Closed test: **12 testers opted in, 14 continuous days**
- [ ] ≥ 3 of them real tradesmen *(release gate, `docs/06` §10 — not a Play rule,
      a quality one)*
- [ ] Apply for production access — answer with specifics about who tested it and
      what changed, not boilerplate
- [ ] Production, **staged rollout 20%** → watch Android Vitals 48 h → 100%

---

# 🤖 Mine

- [x] Free: every paywall limit lifted behind `Monetization.PAYWALL_ENABLED`
- [x] `firstRunAt` recorded, so a future paywall could grandfather today's users
- [x] Purchase UI and "Restore purchases" hidden; billing connection suppressed
- [x] Ads: banner + interstitial every 5th optimize, 10-minute minimum gap
- [x] UMP consent, re-enterable from Settings
- [x] Crashlytics, release builds only
- [x] All 8 critical-path E2E tests, plus "no paywall anywhere" and "PDF is free"
- [x] Waste-% baselines across the whole oracle set
- [x] Secrets scanned across git history · dependency licences checked
- [x] Release signing, keystore generated, signed AAB verified from the bundle
- [x] Store listing copy, screenshots, feature graphic, `app-ads.txt`
- [ ] 🔴 Oracle case **`O-10`** — *blocked on a real tradesman job*. Send me one
      and it becomes a permanent regression test
- [ ] Publisher name in the privacy policy and About screen — **only if you want
      to trade under something other than your own name**. Left alone otherwise

---

## Sequence

```
NOW    keystore backups · app-ads.txt repo · captions        🧑 an hour
       low-end device testing                                🧑 🔴
       ↓
       $25 → account → identity verification                 🧑 ⏱ days
       ↓
       upload AAB · listing · declarations                   🧑 an hour
       ↓
       CLOSED TEST — 12 testers, 14 continuous days          ⏱ 2 weeks
       ↓
       production access application                         ⏱ up to 7 days
       ↓
       staged rollout 20% → Vitals 48 h → 100%
```

**Realistic floor: about three weeks from the day you pay**, and none of it is
code. The single highest-value thing you can do is open the account early — every
other item either is already done or can be done while the clock runs.
