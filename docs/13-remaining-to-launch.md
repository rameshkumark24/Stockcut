# Everything remaining until the app is live on Play

**Rewritten 2026-08-08**, after two decisions that changed most of this list:

1. **v1 ships completely free**, earning only from AdMob — see
   [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).
2. **v1 publishes on a friend's Play Console account**, which already has
   production access.

Together those delete the three slowest items that used to gate everything: the
$25 account, the 14-day closed test, and the billing test matrix.

**🧑 you** · **🤖 me** · **⏱ calendar time that effort cannot compress**

---

## Where things stand

| | Status |
|---|---|
| Phase 0 Scope · Phase 1 Docs | ✅ |
| Phase 3 `:units` + `:optimizer` | ✅ |
| Phase 4 `:data` | ✅ |
| Phase 5 UI — all 7 screens, share, PDF | ✅ |
| Phase 6 Ads + consent + review prompt | ✅ |
| Phase 6 Billing | ⬛ **written, switched off** — v1 sells nothing |
| Phase 7 Feedback channel | ⬛ removed — app collects nothing |
| Phase 8 Testing & QA | ✅ except real-device items |
| Phase 9 Store readiness | 🟡 needs the listing |
| Phase 10 Closed test | ⬛ **not required** — the account has production access |
| Phase 11 Launch | ❌ |

Release APK **4.1 MB** against a 12 MB budget (NFR-3).

---

## What is actually left

**Everything that can be produced off the Play Console is done** — signed AAB,
keystore, listing copy, screenshots, feature graphic, `app-ads.txt`. Step-by-step
upload instructions: [`17-upload-day-runbook.md`](17-upload-day-runbook.md).

Three things remain, and none of them costs money.

1. 🔴 **Back up the keystore twice, off this machine.** It is generated and
   working; it is not backed up. This is the only irreversible mistake available.
2. 🔴 **A low-end real device** — the emulator cannot answer performance or
   auto-backup questions honestly.
3. **Your friend's developer name**, so the privacy policy and About screen name
   the right publisher. Send it and I will update both.

---

# 🧑 Yours

## Publishing on your friend's account

Before anything else, confirm with him:

- [ ] **Policy status is clean** — no strikes, no suspended apps. A strike means
      walk away: terminations are account-wide, and a burned package name
      (`com.measure.stockcut`) is burned permanently
- [ ] He understands **StockCut's compliance becomes his account's risk**
- [ ] He invites `rameshkumaroff@gmail.com` under **Users and permissions**,
      scoped to this app: store presence, testing tracks, production releases,
      reply to reviews
- [ ] **Developer website** on the listing points at *your* GitHub Pages root —
      this is what lets your AdMob account verify the app
- [ ] His **verified developer name**, so the privacy policy and About screen name
      the same publisher
- [ ] Written agreement (WhatsApp is fine, dated): he transfers the app to your
      account once you have the $25, and won't unpublish or edit without asking

**No payments profile needed.** The app sells nothing, so no money passes through
his account at all — that is the whole reason v1 is free.

## AdMob — your account, your money

- [ ] Create a repo named **`rameshkumark24.github.io`** — `app-ads.txt` must sit
      at the **root** of the domain, and the current Pages site is a project page
      (`/Stockcut/`), which will not do
- [ ] Publish `app-ads.txt` there with your AdMob publisher line
- [ ] Add the app in AdMob and verify ownership against that domain
- [ ] AdMob address verification (a PIN posted to you) triggers at $10 earned;
      payout threshold is $100

## Keystore

- [x] `upload-keystore.jks` generated — RSA 4096, alias `upload`, 10,000 days
- [x] `keystore.properties` filled in, both git-ignored
- [x] Release AAB builds signed and verifies
- [ ] 🔴 **Back up the file and its passwords in two places off this machine**
- [ ] Enrol in Play App Signing at first upload
- [ ] Keep it yourself — your friend never needs it, and it survives the transfer

## Store listing

All written and length-checked in
[`16-store-listing.md`](16-store-listing.md); assets in `store/`.

- [x] Title, short description, full description
- [x] **5 screenshots** captured from the real app, with captions
- [x] Feature graphic **1024×500**
- [x] Content rating / ads / IAP / data safety answers all decided
- [ ] Paste it in and **burn the captions into the images**
- [ ] App icon 512×512 — export from `mipmap-xxxhdpi`

## Real-device testing — I cannot do these

- [ ] 🔴 **Low-end real device** (2–3 GB RAM, Android 8–10) — *your actual user's
      phone*
- [ ] 🔴 **Uninstall → reinstall → jobs restored** via auto-backup. The rules are
      written and have never been proven
- [ ] Cold start **< 1.5 s**, release build on that device
- [ ] **Airplane mode** — full core function, and confirm the ad slot collapses
      rather than leaving a grey box
- [ ] Sunlight — read a cut plan outdoors
- [ ] Process death — background 20 min, return to the same state
- [ ] Split screen

## Testers

Not a gate any more — the account already has production access. Still worth it:

- [ ] ≥ 3 real tradesmen on internal testing before you go to production
      *(release gate, `docs/06` §10)*

---

# 🤖 Mine

- [x] Free-tier gates lifted behind `Monetization.PAYWALL_ENABLED`
- [x] `firstRunAt` recorded, so the future paywall can grandfather v1 users
- [x] Purchase UI and "Restore purchases" hidden — an app that sells nothing must
      not offer to sell
- [x] Billing connection suppressed
- [x] Ads: banner + interstitial every 5th optimize, 10-minute minimum gap
- [x] UMP consent, re-enterable from Settings
- [x] Crashlytics, release builds only
- [x] All 8 critical-path E2E tests, plus "no paywall anywhere" and "PDF is free"
- [x] Waste-% baselines across the whole oracle set
- [x] Secrets scanned across git history · dependency licences checked
- [x] Release signing config, degrading gracefully when no keystore exists
- [ ] 🔴 Oracle case **`O-10`** — *blocked on a real tradesman job*. Send me one
      and it becomes a permanent regression test
- [ ] Store listing copy — say the word

---

## Sequence

```
NOW    confirm friend's account is clean · get the invite      🧑 minutes
       app-ads.txt on a root-domain Pages repo                 🧑 minutes
       keystore + two off-machine backups                      🧑 30 min
       ↓
       I build a signed release AAB                            🤖
       ↓
       upload to Internal testing                              🧑
       low-end device + auto-backup verification               🧑 🔴
       screenshots + listing copy                              🧑🤖
       ↓
       PRODUCTION — no closed test required
       staged rollout 20% → watch Vitals 48 h → 100%
```

**Realistic floor: a few days**, gated on the keystore, a real device, and
screenshots. Not weeks — the two multi-week items are both gone.

## Then, at about month 6

Transfer the app to your own account, *then* switch the paywall on. Never the
other way round. Full procedure in
[`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).
