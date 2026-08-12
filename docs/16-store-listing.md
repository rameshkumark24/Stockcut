# Play Store listing — final copy

**Ready to paste.** Lengths, counts, layout and encoding are all verified by
`tools/check-listing-lengths.ps1` — run it after any edit.

> 🔴 **Copy every field from a UTF-8 aware editor** (VS Code), never from a
> terminal. The copy uses em dashes (U+2014) and bullets (U+2022); a tool reading
> this file as Windows ANSI turns each into two junk characters. That is not
> hypothetical — it has happened twice here, once to the length checker and once
> to the checker's own source.
>
> **The title is the easy case**: 29 characters, damage obvious at a glance. The
> **full description is the dangerous one** — roughly fifteen em dashes and ten
> bullets, now on long single lines, where a mangled character mid-paragraph is
> easy to miss and no length check can catch it (2,238 plus mojibake is still
> well under 4,000).
>
> After pasting each field into Play Console, look for `—` and `•`, not
> `â€`.

ASO is this app's entire distribution channel — there is no marketing budget, no
website traffic and no social following. The store listing *is* the funnel.

---

## App name — 29 / 30

```
StockCut — Cut List Optimizer
```

> If Play rejects the em dash (it occasionally objects to non-ASCII in titles),
> use `StockCut - Cut List Optimizer` — 29 characters, same meaning.

**Do not** add "Free", "Best", "#1" or emoji. Play's metadata policy prohibits
promotional text and ranking claims in the title, and it is a common rejection.

---

## Short description — 76 / 80

The single highest-weight ASO field. It is also the only text shown before
someone taps "Read more", so it must carry the whole proposition.

```
Cut list optimizer for metal and timber. Kerf-accurate, offline, no sign-up.
```

**Why these words.** "Cut list optimizer" is the phrase tradesmen actually
search. "Kerf-accurate" is the credibility signal — it is the detail that tells a
fabricator this was built by someone who has used a saw. "Offline" and "no
sign-up" pre-empt the two objections that stop installs on utility apps.

---

## Full description — 2,238 / 4,000

```
Stop working out cut lists on the back of a delivery note.

Tell StockCut the lengths you can buy and the pieces you need. It works out which piece comes off which bar, in seconds, with as little offcut as it can find — and it counts your saw blade, which is where hand-written cut lists go wrong.

WHAT IT DOES

• Works out how to cut the job from fewer bars
• Accounts for kerf — every cut eats blade width, and ten cuts at 3 mm is 30 mm
• Shows each bar as a diagram, so you can read it at the saw
• Tells you the offcut left on every bar and the total waste percentage
• Warns you when a piece is longer than anything you can buy, instead of quietly dropping it

METRIC AND IMPERIAL

Millimetres, centimetres, metres, decimal inches, and fractional inches. Type 1 5/16" and it takes it — no converting to decimals in your head, no rounding errors creeping into a job.

BUILT FOR THE SITE, NOT THE OFFICE

• Works with no signal. Everything runs on the phone
• No account, no sign-up, no password
• Your jobs are never uploaded — they stay on your device
• Large type and big touch targets, for reading with safety glasses on
• Share a cut plan as an image or a PDF — send it to the apprentice, or print it

WHO IT IS FOR

Fabricators, welders, metalworkers, carpenters, timber framers, shopfitters, window and door installers, fencers, and anyone cutting from linear stock: steel tube, box section, angle, flat bar, rebar, aluminium extrusion, timber studs, skirting, trim, pipe, and conduit.

If you buy material in fixed lengths and cut pieces from it, this saves you material on every job.

HOW IT WORKS

1. Enter the lengths you can buy — 6 m bars, 8 ft studs, whatever your supplier sells
2. Enter the pieces you need and how many
3. Set your blade width
4. Tap Optimize

You get a plan: bar by bar, cut by cut, with the offcut on each.

FREE

StockCut is free and supported by ads. There is no subscription, no trial, and nothing is locked. Ads are kept deliberately light — a banner, and an occasional full-screen ad after you tap Optimize, never while you are entering a job.

WHAT IT IS NOT

This is a 1D linear cut list optimizer. It solves bar, tube and length cutting. It does not do 2D sheet nesting for plywood or plate.
```

---

## Screenshot captions

Play shows these under each image on some surfaces, and burned-in captions are
what actually get read. **Burn the caption into the image**, top or bottom
third, and keep the app UI legible below it.

**Upload in this order** — it is not the filename order. Most installs are decided
on the first image without scrolling.

| Order | File | Caption |
|---|---|---|
| 1 | `01-cut-plan.png` | `See exactly which piece comes off which bar` |
| 2 | `05-fractional-inches.png` | `Type 1 5/16" — no converting to decimals` |
| 3 | `02-parts-metric.png` | `Metric or imperial. Your call, per job` |
| 4 | `03-stock.png` | `Tell it the lengths your supplier sells` |
| 5 | `04-jobs.png` | `Works with no signal. Nothing leaves your phone` |

Files are in [`store/screenshots/`](../store/screenshots/), 1080×2400, captured
from the real app by `StoreScreenshotTest`.

### Two things worth knowing about these images

**The cut plan shows 2.0% waste, and that is a real optimizer result** — 7 bars,
mixed lengths, computed from the job seeded in the test. The first attempt used
round part lengths and produced **16.1%, labelled "High waste" in red**: a hero
image for a waste-reduction tool advertising bad packing. The lengths were then
chosen by searching for a job that is both low-waste and visually mixed, which is
why bar 1 reads `2400 · 2400 · 1190` rather than three identical blocks.

**No ad banner appears.** The captures were taken in airplane mode, so the banner
never loaded — and the app renders nothing rather than a grey placeholder, which
is the designed behaviour. This is normal for store screenshots and is not a
misrepresentation: **"contains ads" is declared on the listing**, which is where
Play requires the disclosure.

### Re-capturing them

```
adb shell cmd connectivity airplane-mode enable
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.stockcut.StoreScreenshotTest \
    com.measure.stockcut.debug.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.measure.stockcut.debug/files/screenshots/. store/screenshots/
```

🔴 Run it with `am instrument`, **not** `./gradlew connectedAndroidTest` — Gradle
uninstalls the app when the run finishes and takes the PNGs with it.

---

## App icon — 512 × 512

[`store/play-store-icon-512.png`](../store/play-store-icon-512.png)

There was nothing to export: the launcher icon is an adaptive icon defined purely
in XML, with **no raster asset anywhere in the project**. So it is rendered from
the same vector and colour the app itself uses (`tools/make-store-icon.py`),
which is what stops the store icon and the launcher icon drifting apart.

It is cropped to the adaptive icon's **72×72 safe zone**, not the full 108×108
viewport. Launchers mask the outer ring away, so the full viewport would have put
a noticeably smaller bar on the listing than the one the same user finds on their
home screen a minute later.

## Feature graphic — 1024 × 500

[`store/feature-graphic.png`](../store/feature-graphic.png)

The cut-plan bar visual, the app name, and one line. No screenshot of the app
inside it — Play crops and scales this asset unpredictably across surfaces, and
UI shrunk into it becomes unreadable mush.

---

## Categorisation and declarations

| Field | Answer |
|---|---|
| App category | **Tools** |
| Tags | Utilities, Productivity |
| Content rating | Everyone — no user content, no communication, no purchases |
| Target audience | 18+ (trade tool; keeps you out of Families policy entirely) |
| **Contains ads** | **Yes** — 🔴 mandatory, and Play checks it automatically |
| **In-app purchases** | **No** — v1 sells nothing |
| Government app | No |
| Financial features | None |

### Data safety form

The app collects nothing from the user. Everything below comes from the two SDKs.

| Question | Answer |
|---|---|
| Does your app collect or share user data? | **Yes** — via AdMob and Crashlytics |
| Device or other IDs | **Collected and shared** — 🔴 the **Advertising ID**, by AdMob |
| Crash logs | **Collected**, not shared — Crashlytics |
| Diagnostics | **Collected**, not shared — Crashlytics |
| Purpose | Advertising and marketing · Analytics · Crash reporting |
| Is data encrypted in transit? | **Yes** |
| Can users request deletion? | **Yes** — via the privacy policy contact |
| Data collected is optional? | Ad ID is subject to consent (UMP) where required |

🔴 **The AD_ID declaration is not optional and not cosmetic.** The permission is
in the merged manifest, Play scans for it, and an app that declares "no
advertising ID" while shipping AdMob gets rejected — or removed after the fact.

## The two URLs the listing needs

| Field | Value |
|---|---|
| **Privacy policy** | `https://rameshkumark24.github.io/Stockcut/privacy-policy.html` |
| **Developer website** | `https://rameshkumark.vercel.app` |

They are deliberately on different hosts and both are correct.

🔴 **Developer website is not cosmetic** — it is the domain AdMob crawls for
`app-ads.txt`, which is already published at
[`https://rameshkumark.vercel.app/app-ads.txt`](https://rameshkumark.vercel.app/app-ads.txt).
Point this field anywhere else and the file stops being found, because crawlers
only ever read a domain **root** and never a subdirectory.

---

## Publisher name

The listing shows the **verified developer name on your own Play Console
account**, which is whatever name you verify with. Nothing in the repo needs
changing for that — the privacy policy and About screen do not name a company,
so they stay correct however you verify.

The one case that does need a change: if you decide to trade under a **business
name** rather than your own. Then the privacy policy's "who we are" line and the
About screen should say the same thing as the listing. Tell me the name and I
will update both in one commit — a mismatch between the listing and the privacy
policy is a data-safety inconsistency, which is the category Play acts on
fastest.
