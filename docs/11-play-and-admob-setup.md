# Play Console + AdMob setup — the detailed guide

Everything in here needs your Google account, so none of it can be done for you.
Each step ends with **"send me"** where the code needs a value back.

> ⚠️ **Part 1 does not apply to v1.** v1 ships free — there is no in-app product
> to create. Come back to Part 1 when the app has been transferred to your own
> account and the paywall goes on
> ([`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md)).
>
> **Parts 2–4 (AdMob, consent, keystore) are all still current**, and the AdMob
> part matters more than ever: it is now the app's only revenue. One addition —
> `app-ads.txt` must sit at the **root** of the domain named in the listing's
> Developer website field, which means a repo called
> `rameshkumark24.github.io`, not the `/Stockcut/` project page.

---

# Part 1 — The in-app product

**Prerequisite:** Play Console account exists, and an app entry has been created
with the final package name.

## 1.1 You must upload a build first

This trips people up: **the Monetise section stays greyed out until an APK or AAB
using Play Billing has been uploaded to at least one track.** You cannot create
the product on an empty app entry.

So the order is: billing code merged → build a release AAB → upload it to
**Internal testing** → *then* create the product.

That is fine, because Internal testing is instant and separate from the 14-day
closed test.

## 1.2 Create the product

Play Console → your app → **Monetise with Play** → **Products** → **In-app products** → **Create product**

| Field | Value | Why |
|---|---|---|
| Product ID | `stockcut_unlock` | 🔴 **Permanent.** Cannot be changed or reused after creation, even if you delete the product. Lower-case, no spaces. |
| Name | `StockCut Unlock` | Shown at purchase |
| Description | `Unlimited parts, unlimited jobs, PDF export, no ads. One time.` | Shown at purchase |
| Type | **One-time product**, non-consumable | 🔴 **Not** a subscription, and it must **not** be consumable — a consumable can be bought again and would not persist |
| Price | **$4.99** USD | Play auto-converts for other countries; review the India price, as auto-conversion can land somewhere odd |
| Status | **Active** | A product left inactive returns "item unavailable" and looks like a bug |

**→ Send me the Product ID.** The billing code needs it exactly.

## 1.3 Configure license testers

Play Console → **Settings** (left nav, account level, *not* the app) → **License testing**

1. Add the Gmail addresses that should be able to test purchases. Your own
   account and 2–3 of your testers is plenty.
2. Set **License response** to `RESPOND_NORMALLY`.
3. These accounts see purchases as **test purchases** — real flow, real
   acknowledgement, **no money charged**, and they can buy repeatedly.

⚠️ The tester must be signed into the Play Store on the device **with that exact
account**. A different signed-in account gets charged for real.

## 1.4 Payments profile

Play Console → **Setup** → **Payments profile**

Bank account and tax details. Required before you can receive anything, and it
takes days to verify — start it early even though no revenue exists yet.

🔴 **For India specifically:** confirm the export-of-service treatment of foreign
app revenue **with a CA** before revenue arrives (`docs/00-gap-audit` §B4). Do not
guess at this one.

## 1.5 What gets tested afterwards

Once the above exists, the billing matrix in `docs/06` §5 can actually be walked.
All of it must pass before release:

- Purchase with a license tester → unlocks immediately, ads gone
- Purchase, then **force-stop and reopen offline** → still unlocked
- Purchase, **uninstall, reinstall, Restore purchases** → unlocks
- Purchase cancelled mid-flow → no entitlement change, **no nag**
- Refund issued in Console → entitlement revoked on next *online* launch
- Billing unavailable → app fully usable free, retry message shown
- 🔴 Purchase **acknowledged immediately** — Google auto-refunds anything
  unacknowledged after 3 days, so this is a money bug, not a polish bug

---

# Part 2 — AdMob

## 2.1 Create the account

<https://admob.google.com> — sign in with the **same Google account** as Play.
Keeping them together avoids a payments mess later.

## 2.2 Add the app

AdMob → **Apps** → **Add app** → Android.

- If the app is not published yet, choose **"No, it's not listed on a store yet"**
  and add it manually. You can link it to the Play listing later.
- You will get an **App ID** shaped like `ca-app-pub-################~##########`
  *(note the `~`)*.

**→ Send me the App ID.** It goes in the manifest via a placeholder, never
hardcoded.

## 2.3 Create two ad units

AdMob → your app → **Ad units** → **Add ad unit**

| # | Format | Name | Where it appears |
|---|---|---|---|
| 1 | **Banner** | `StockCut banner` | Bottom of the projects list and editor |
| 2 | **Interstitial** | `StockCut interstitial` | After every 3rd optimize, free tier only |

Each gives a **Ad unit ID** shaped `ca-app-pub-################/##########`
*(note the `/`, not `~` — they are different things and easy to confuse)*.

**→ Send me both Ad unit IDs.**

They go in `local.properties`, injected via `buildConfigField`, and are never
committed (`docs/02` §7). Assume they become public anyway — a decompiled APK
gives them up, and neither is secret.

## 2.4 🔴 The rule that ends accounts

**Google's test ad unit IDs are used in every non-production build. Always.**

Clicking a live ad in your own app **terminates the AdMob account permanently and
forfeits all earnings**. There is no appeal worth relying on. This is `CLAUDE.md`
rule 8 and it is a hard rule.

The build is already structured so debug and closed-test variants use test IDs
and only the production variant uses yours. Do not "just try the real one to see
if it works."

## 2.5 UMP consent — not optional

You are deliberately targeting US/UK/CA/AU traffic, so EU/UK users will arrive.
The **UMP SDK consent flow must run before any ad request**, and it must be
re-reachable from Settings.

AdMob → **Privacy & messaging** → **GDPR** → create a consent message, and also
create the **US state regulations** message while you are there.

Testing it requires an EU IP — a VPN is the normal way. `docs/06` §5 makes this a
release gate.

## 2.6 App ads.txt

AdMob will nag about `app-ads.txt`. It only applies if you have a website listed
on your Play listing. If you have no website, there is nothing to do and the
warning is expected.

---

# Part 3 — Firebase / Crashlytics

Needs your Google account too.

1. <https://console.firebase.google.com> → **Add project** → same Google account
2. Add an **Android app** with the final package name
3. Download **`google-services.json`** and put it in `app/`

🔴 It is already in `.gitignore` — leave it there. It is technically shippable
but does not belong in a public repo.

4. In the Firebase console, enable **Crashlytics**

---

# Part 4 — The keystore *(do this yourself)*

I can run the command, but I should not: the password would end up in a chat
transcript, and for this key **leaking it is as damaging as losing it**.

> 🔴 **Losing the upload keystore means the app can never be updated again.**
> Not "hard to recover" — impossible.

## Generate it

Run this in the repo root. It will prompt for a password twice and then a few
name fields you can answer however you like:

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

`keytool` ships with the JDK, which you already have — it is on your PATH
alongside `java`.

## Record it

Copy `keystore.properties.example` → `keystore.properties` and fill in the path,
alias and passwords. **Neither file is ever committed** — both are already in
`.gitignore`.

## Back it up — twice, off this machine

This is the step people skip and it is the one that is unrecoverable.

1. **Password manager** — store the `.jks` file itself as an attachment *and* the
   passwords
2. **Encrypted cloud folder** — a second copy somewhere physically different

Not: this laptop, and not this git repo.

## Enrol in Play App Signing

At first upload, accept **Play App Signing**. Google then holds the *app signing*
key and you hold only the *upload* key — which means that even if the worst
happens, there is a recovery path with Google rather than none at all.
