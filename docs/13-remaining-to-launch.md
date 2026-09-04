# Everything remaining until the app is live on Play

**Rewritten 2026-08-08.** Two settled decisions shape this list:

1. **StockCut is completely free**, earning only from AdMob — see
   [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).
2. **It publishes on the owner's own Play Console account**, opened when the $25
   is available.

**🧑 you** · **🤖 me** · **⏱ calendar time that effort cannot compress**

---

## Where this actually stands — 13 Aug 2026

**Submitted and PUBLISHED to closed testing the same day.** Submission 1 covered
the release, store listing, all 10 App content declarations and store settings.
Google's dialog warns of up to seven days; it took hours.

The app is live on the **Alpha** closed track, delivered at **3.76 MB** per
install (the AAB splits by ABI). Not publicly listed — only the 16 invited
testers can install it.

| Submission | Contents | Status |
|---|---|---|
| 1 | v1.0.0 (1), store listing, all 10 App content declarations, store settings | Published |
| 2 | Feedback channel on the track (`rameshkumaroff@gmail.com`) | Published |
| 3 | **v1.0.1 (2)** — removed a dead "Not built yet / Restore purchases arrives with in-app billing" placeholder from About, added a "How it works" section | In review |

Two things that submission 3 illustrates and are worth remembering:

- **App text can only be changed by shipping a build.** Store listing text is
  edited in the console with no build at all; anything compiled into the APK is
  not. That is why a one-line text fix cost a full release cycle.
- **`versionCode` must increase for every upload**, even a text fix. Play
  rejects same-or-lower and remembers the highest it has ever seen.

### What is running now

- ⏱ **The 14-day clock**, which starts when **12 testers are installed and opted
  in** — not when the app was published. Earliest possible finish is 27 Aug 2026,
  and only if all 12 install on day one.
- **Pre-launch report** — Google runs the app on real physical devices after
  every upload, free, and reports crashes, ANRs, accessibility and performance
  with screenshots. **Read it.** It is the closest thing available to the
  low-end-device testing that is still outstanding below.

### Shipping updates during the test

Local builds never reach testers. To push a fix:

1. Bump `versionCode` in `app/build.gradle.kts` (now 1, next 2). Play rejects
   same-or-lower and remembers the highest number it has ever seen.
2. `./gradlew :app:bundleRelease`
3. Upload to the same closed track, send for review — updates review faster.
4. Testers get it automatically; Play auto-updates by default.

🔴 **Uploading an update does NOT reset the 14 days.** Google counts testers
opted in continuously, not the build sitting still. Fix things during the test
rather than freezing the app out of fear. What *does* hurt is a tester
uninstalling — that drops the count below 12.

Store listing text and screenshots can be changed with no new build at all.

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

- [x] 🔴 **Back up `upload-keystore.jks` and its passwords off this machine** —
      done 2026-08-08. Losing it would mean the app could never be updated again
- [x] **`app-ads.txt` published** — 2026-08-09, at
      [`https://rameshkumark.vercel.app/app-ads.txt`](https://rameshkumark.vercel.app/app-ads.txt),
      verified live with `Content-Type: text/plain`.

      It is served from the owner's portfolio (`My-Portfolio` repo, `public/`,
      which Vite copies to the deploy root) rather than the GitHub Pages user
      site this doc previously specified. `vercel.app` is a public-suffix domain,
      so `rameshkumark.vercel.app` counts as a root and the file resolves without
      a subdirectory. That removed the need for a `rameshkumark24.github.io` repo
      entirely.

      🔴 If AdMob still reports "app-ads.txt not found" a few days after the
      listing is live, the crawler is not treating the Vercel subdomain as a
      root. Fallback: the same one file on a `rameshkumark24.github.io` repo.
      Not blocking either way — it affects ad demand quality, not whether ads
      serve or whether you get paid.
- [ ] Line up your **15 testers** and confirm they will install from a Play link
      when asked. Invited-but-not-installed does not count toward the 12
- [ ] Burn the captions into the 5 screenshots
- [x] 512×512 store icon rendered — `store/play-store-icon-512.png`

## 🚀 LAUNCHED — 4 Sept 2026

**StockCut is live and publicly listed on Google Play.**
`https://play.google.com/store/apps/details?id=com.measure.stockcut`
Verified reachable: HTTP 200 (an unknown package name returns 404).

Submission 6, 4 Sept 2026 00:33 — Production — **Published**. Full rollout;
Play does not offer a staged percentage for a first production release, so this
went straight to 100% with no staged safety net.

Shipped artifact: **1.0.3 (versionCode 4)**, 3.77 MB per install, targetSdk 36,
minSdk 26, 177 countries.

### 🔴 The rule that changes today

Every build before this one carried Google's TEST ad IDs, which made clicking an
ad in your own app harmless. **That protection is gone.** 1.0.3 carries the real
publisher (`pub-7038016776482334`), so a click by the owner or by a friendly
tester is invalid traffic — and AdMob's answer to invalid traffic is permanent
account termination with forfeiture of unpaid earnings (CLAUDE.md rule 8).

The 15 closed testers are the highest-risk group precisely because they want to
help. **They must be told.** If ad behaviour ever needs testing again, build a
test-ID APK (`./gradlew :app:assembleDebug`, no flag) rather than tapping a live
ad.

### Watch list

| When | Where | Act if |
|---|---|---|
| 24 h | AdMob → impressions | 🔴 **Zero on real installs** = test IDs shipped after all |
| 48 h | Android vitals | Crash > 1.09% or ANR > 0.47% — either demotes the listing |
| ~1 week | AdMob → app-ads.txt | Still "not found" → use the fallback in this doc |

---

## Android developer verification — DONE, no action needed

Google's 15 July 2026 rule: any Play app whose **package name and signing keys**
are not registered against a verified developer identity by **30 September 2026**
is removed from Google Play globally, and eventually becomes uninstallable on
certified devices in some countries even when sideloaded.

Checked 31 Aug 2026 — Play Console → Android developer verification:

| Package | Status | Keys | Last updated |
|---|---|---|---|
| `com.measure.stockcut` | ✅ **Registered** | 3 | 10 Aug 2026 |

Registered automatically from the Play Console account when the app was created,
because identity verification was already complete and Play App Signing was
accepted. **The deadline is met and there is nothing to do.**

🔴 Do NOT use "Register package name". That is for apps distributed outside Play
or signed with additional keys, and StockCut is neither — it ships only through
Play, signed by one upload key with Play App Signing on top. A stray entry would
be noise at best.

---

## Real-device verification of 1.0.2 — 31 Aug 2026

Run on the **vivo V2307, Android 15, 3-button navigation, dark theme**, debug
build of **1.0.2 (versionCode 3)**, installed alongside the Play copy as
`com.measure.stockcut.debug` so the store install and its data were untouched.

| Check | Result |
|---|---|
| Setup → End trim with the keyboard up | ✅ form lifts, field clear of the IME. **This is the one the owner confirmed broken on this exact phone** |
| Save trim reachable while typing | ✅ the form scrolls; before `consumeWindowInsets` it had zero scroll range |
| Invalid trim text does not wipe a saved trim | ✅ saved 50, entered `1.2.3`, tapped Save trim, left and re-entered — still 50. Under the old code this wrote 0 |
| Ad banner clears the 3-button navigation bar | ✅ |
| Optimize clears the navigation bar | ✅ |
| Status bar icons legible in dark theme | ✅ |

🔴 Two items from the 1.0.2 change set are still UNVERIFIED on hardware: the
**display-cutout union** on the ad banner (needs a corner-cutout phone in
landscape; the V2307 did not exercise it) and behaviour at **large font scale**,
which was checked on the emulator at 1.3x but not here.

---

## Real-device testing

Run on a **vivo V2307, Android 15, 8 GB RAM, 3-button navigation**, release build,
on 2026-08-08. It found two real bugs that no emulator run had shown — see below.

- [x] **Uninstall → reinstall → jobs come back** via auto-backup. **Proven.** A
      real uninstall then restore brought back every job including a user-created
      one, and the example did not double-seed, so DataStore restored too
- [x] Cold start **670 ms median** (release build, 5 runs) against a 1.5 s budget
- [x] No crashes, no ANRs, no StrictMode violations in logcat
- [x] Ad banner and Optimize button clear the navigation bar *(they did not — see
      below)*
- [ ] 🔴 **Low-end phone** (2–3 GB RAM, Android 8–10) — still outstanding. The
      V2307 is a *good* phone; it proves correctness on real hardware, not
      performance on slow hardware. 670 ms here could be 2 s there
- [ ] Read a cut plan outdoors in sunlight
- [ ] Background it 20 minutes, return to the same state
- [ ] Split screen

### 🔴 Two bugs found only by running on a real phone

Both were invisible on the emulator, and for the same underlying reason: the
emulator used **gesture navigation** and had its system theme matching the app's.

**1. The Optimize button was under the navigation bar.** `Scaffold` does not
apply window insets to an arbitrary composable in the `bottomBar` slot — that is
the slot's own job — and the button had only a fixed padding. With 3-button
navigation the Home and Back keys were drawn *on top of* the app's primary
action, so tapping the lower half of Optimize left the app. The ad banner had the
same fault, which is additionally an AdMob policy problem: a partly obscured ad
next to system buttons is what invalid-traffic enforcement looks for.
Fixed with `navigationBarsPadding()` on both.

**2. Status bar icons were invisible.** `enableEdgeToEdge()` picks icon colour
from the **system** dark-mode setting, not the app's theme. Phone in dark mode,
app set to Light — a combination this app explicitly offers in Settings — gave
white icons on a white background, so the clock and battery vanished. Fixed with
a `SideEffect` in `StockCutTheme` that sets `isAppearanceLightStatusBars` from
the app's own resolved theme.

**The lesson worth keeping:** every emulator screenshot in `store/` was taken
with gesture navigation. Gesture nav hides inset bugs, because the pill is short
enough that a fixed padding looks fine. Test 3-button navigation.

## ✅ Done 13 Aug 2026 — account through to publication

- [x] Play Console account created and identity-verified
- [x] App created as `com.measure.stockcut`, App, **Free** *(permanent: a free
      app can never be made paid; in-app purchases could still be added)*
- [x] `app-release.aab` uploaded, **Play App Signing accepted** — so a lost
      upload key is now recoverable rather than fatal
- [x] Store listing, icon, feature graphic and 5 screenshots
- [x] All 10 App content declarations, including AD_ID in data safety
- [x] Developer website set to `https://rameshkumark.vercel.app`
- [x] Countries: all · Testers: the 15-address list, attached to the track
- [x] Submitted and **published**

## ⏱ Then the part that is pure waiting

- [ ] Send the opt-in link to the 15, **after installing from it yourself**
- [ ] Read the **pre-launch report** — free real-device results, already waiting
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
