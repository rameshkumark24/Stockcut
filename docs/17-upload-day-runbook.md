# Upload day — the runbook

**Everything that can be done off the Play Console is done.** This is what happens
in the console, in order, with the file to use at each step.

No step here needs code from me. Where a step asks for a value, it is in this repo.

---

## Before you start — what already exists

| Thing | Where |
|---|---|
| Signed release bundle | `app/build/outputs/bundle/release/app-release.aab` |
| Upload keystore | `upload-keystore.jks` *(git-ignored)* |
| Keystore passwords | `keystore.properties` *(git-ignored)* |
| Listing copy | [`16-store-listing.md`](16-store-listing.md) |
| Screenshots ×5 | `store/screenshots/` |
| Feature graphic | `store/feature-graphic.png` |
| `app-ads.txt` | `store/app-ads.txt` |
| Privacy policy | `https://rameshkumark24.github.io/Stockcut/privacy-policy.html` |

The bundle is **v1.0.0, versionCode 1**, `com.measure.stockcut`, targetSdk 36,
minSdk 26 — all verified inside the built AAB, not just in Gradle.

---

## 🔴 Step 0 — back up the keystore. Do this today, not on upload day.

If `upload-keystore.jks` is lost, **StockCut can never be updated again**. Not
patched, not renamed, not fixed. The only remedy is a new package name and
starting from zero installs.

- [ ] Copy `upload-keystore.jks` to a cloud drive that is not this machine
- [ ] Copy it to a second place — a USB stick, or a different cloud account
- [ ] Put the passwords from `keystore.properties` into a password manager
- [ ] Open the backup from another device to prove it is really there

This does not wait for the $25. It is the one irreversible mistake available.

---

## Step 1 — app-ads.txt, so AdMob can verify the app

Also free, also does not wait. AdMob takes a day or two to crawl, so doing it
early means it is already verified when the listing goes up.

- [x] **Done 2026-08-09.** It is served from the owner's portfolio at
      `https://rameshkumark.vercel.app/app-ads.txt`, verified live with
      `Content-Type: text/plain`.

Nothing to do here unless AdMob later reports the file as missing — see
[`13-remaining-to-launch.md`](13-remaining-to-launch.md) for the fallback.

It is normal for AdMob to say "not found" for the first day or two.

---

## Step 2 — the account ⏱

**This is the critical path. Everything downstream waits on it.**

- [ ] Pay the **$25** and create the Play Console account
- [ ] Complete **identity verification** — days, not minutes
- [ ] Create the app: **StockCut — Cut List Optimizer**, `com.measure.stockcut`,
      type **App**, **Free**

🔴 The package name is permanent. `com.measure.stockcut` can never be changed or
reused once published.

---

## Step 3 — the release

- [ ] **Closed testing** → create a track → upload `app-release.aab`
- [ ] Accept **Play App Signing** when offered. Google holds the app signing key;
      your upload key stays yours
- [ ] Install from the test link on a real phone and confirm it launches

---

## Step 4 — the listing

Paste from [`16-store-listing.md`](16-store-listing.md). Every field is
length-checked.

- [ ] App name, short description, full description
- [ ] 5 screenshots **in the order given in that doc**, captions burned in
- [ ] Feature graphic
- [ ] App icon 512×512 — export from `app/src/main/res/mipmap-xxxhdpi`
- [ ] Category **Tools**, contact email, privacy policy URL
- [ ] **Developer website** → `https://rameshkumark.vercel.app` (this is what
      AdMob verifies against, and it already serves `app-ads.txt`)

🔴 After pasting the title, **look at it**. If it shows `â€"` instead of `—`, your
editor mangled the encoding — retype the dash.

---

## Step 5 — declarations, where apps actually get rejected

- [ ] Content rating questionnaire → expect **Everyone**
- [ ] **Ads: Yes, contains ads**
- [ ] **In-app purchases: No**
- [ ] **Data safety** — the full table is in
      [`16-store-listing.md`](16-store-listing.md#data-safety-form)
- [ ] 🔴 Declare the **Advertising ID**. AdMob puts `AD_ID` in the manifest, Play
      scans for it, and "no advertising ID" with AdMob present is a rejection — or
      a removal after the fact

**No payments profile is needed.** The app sells nothing.

---

## Step 6 — ⏱ the closed test. Two weeks, and nothing shortens it.

- [ ] Get **12 testers opted in via the Play link and actually installed**.
      Invited-but-not-installed does not count
- [ ] Keep **12 opted in for 14 continuous days**. If someone opts out mid-way the
      count drops and the clock effectively restarts — over-recruit; you have 15
- [ ] ≥ 3 real tradesmen among them *(release gate, `docs/06` §10 — our rule, not
      Google's, and the last cheap chance to hear "that's not how we'd cut it")*
- [ ] Apply for **production access**. Answer with specifics — who tested it, what
      they said, what changed. Boilerplate answers get bounced
- [ ] Wait up to **7 days** for review

---

## Step 7 — production

- [ ] Promote to production, **staged rollout at 20%**
- [ ] Watch Android Vitals for 48 hours — crash rate and ANR rate
- [ ] Go to 100%

---

## After launch

- Ad revenue accrues in **your** AdMob account. Payout threshold is $100; address
  verification (a PIN posted to you) triggers at $10
- Expect very little at first. v1's job is users and reviews, not income
- Send me a real job from a tradesman and it becomes oracle case `O-10`
- If you ever want to add a paid unlock, the code is written and dormant:
  [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md)
