# Upload day — the runbook

**Everything that can be done off the Play Console is done.** This is the list of
what happens in the console, in order, with the file to use at each step.

Nothing here needs code from me. If a step asks for a value, it is in this repo.

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
minSdk 26 — all four verified inside the built AAB, not just in Gradle.

---

## 🔴 Step 0 — back up the keystore. Before anything else.

If `upload-keystore.jks` is lost, **StockCut can never be updated again**. Not
patched, not renamed, not fixed. The only remedy is a new package name and
starting from zero installs.

- [ ] Copy `upload-keystore.jks` to a cloud drive that is not this machine
- [ ] Copy it to a second place — a USB stick, or a different cloud account
- [ ] Put the passwords from `keystore.properties` in a password manager
- [ ] Confirm you can open the backup copy from another device

Do not skip this because you are keen to upload. It is the one irreversible
mistake available today.

---

## Step 1 — your friend's account

- [ ] Confirm **no policy strikes** on the account
- [ ] He creates the app: **StockCut — Cut List Optimizer**, `com.measure.stockcut`,
      App, Free
- [ ] He invites `rameshkumaroff@gmail.com` under **Users and permissions**,
      app-scoped: store presence, testing tracks, production releases, reply to
      reviews
- [ ] Get his **verified developer name** and send it to me — the privacy policy
      and About screen must name the same publisher

**No payments profile is needed.** The app sells nothing, so no money goes through
his account.

---

## Step 2 — app-ads.txt, so AdMob pays *you*

This is what links the app on his account to the AdMob account on yours. Skip it
and ad revenue is unverified.

- [ ] Create a GitHub repo named exactly **`rameshkumark24.github.io`**
      *(the `/Stockcut/` project page will not work — `app-ads.txt` must be at the
      domain root)*
- [ ] Add `store/app-ads.txt` to it as `app-ads.txt` at the top level
- [ ] Enable Pages on that repo
- [ ] Confirm `https://rameshkumark24.github.io/app-ads.txt` loads and shows the
      single `google.com, pub-…` line
- [ ] Set the listing's **Developer website** to `https://rameshkumark24.github.io`
- [ ] In AdMob, add the app and verify it against that domain

AdMob takes a day or two to crawl. It is normal for it to say "not found" at first.

---

## Step 3 — the release

- [ ] **Internal testing** → create release → upload `app-release.aab`
- [ ] Accept **Play App Signing** when offered *(your upload key stays yours; Google
      holds the app signing key, and both survive the transfer later)*
- [ ] Install from the internal test link on a real phone and check it launches

---

## Step 4 — the listing

Paste from [`16-store-listing.md`](16-store-listing.md). Everything is
length-checked.

- [ ] App name, short description, full description
- [ ] 5 screenshots **in the order given in that doc**, captions burned in
- [ ] Feature graphic
- [ ] App icon (from the app — Play pulls 512×512 separately; export from
      `app/src/main/res/mipmap-xxxhdpi`)
- [ ] Category **Tools**, contact email, privacy policy URL

🔴 After pasting the title, **look at it**. If it shows `â€"` instead of `—`,
your editor mangled the encoding — retype the dash.

---

## Step 5 — declarations, where apps actually get rejected

- [ ] Content rating questionnaire → expect **Everyone**
- [ ] **Ads: Yes, contains ads**
- [ ] **In-app purchases: No**
- [ ] **Data safety** — the full table is in
      [`16-store-listing.md`](16-store-listing.md#data-safety-form)
- [ ] 🔴 Declare the **Advertising ID**. AdMob ships `AD_ID` in the manifest, Play
      scans for it, and "no advertising ID" with AdMob present is a rejection —
      or a removal after the fact

---

## Step 6 — 🔴 the real device, before production

The emulator cannot answer these, and I could not either.

- [ ] Low-end phone, 2–3 GB RAM — *your actual user's phone*
- [ ] **Uninstall → reinstall → jobs come back** via auto-backup. Written, never
      proven
- [ ] Cold start under 1.5 s on that phone, release build
- [ ] Airplane mode: full function, and the ad slot collapses rather than leaving
      a grey box
- [ ] Read a cut plan outdoors in sunlight
- [ ] Background it 20 minutes, return, same state

Get **3 real tradesmen** on the internal test before production. It is not a Play
requirement — the account already has production access — but it is the last
chance to hear "that's not how we'd cut it" cheaply.

---

## Step 7 — production

- [ ] Promote to production, **staged rollout at 20%**
- [ ] Watch Android Vitals for 48 hours — crash rate and ANR rate
- [ ] Go to 100%

---

## After launch

- Ad revenue accrues in **your** AdMob account. Payout threshold is $100; address
  verification (a posted PIN) triggers at $10
- Expect very little at first. v1's job is users and reviews, not income
- Send me a real job from a tradesman and it becomes oracle case `O-10`

## At about month 6

Transfer the app to your own Play Console account, **then** switch the paywall on.
Never the other way round — the order is the entire reason v1 is free.
Full procedure: [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).
