# Growth release 1.1.0 (code 7): step by step

Everything below is owner-side. The code, the listing copy and the screenshots
are already built and committed on branch `growth-share-and-listing`.

## What is ready

| Item | Where |
|---|---|
| Signed bundle, 1.1.0 (code 7), production AdMob ID verified | `app/build/outputs/bundle/release/app-release.aab` (rebuild with the command in doc 17 if `clean` was run) |
| Captioned screenshots, 1080x1920 | `store/screenshots/captioned/` |
| en-US (default), en-GB, en-IN copy | `store/listing/<lang>/` and `docs/16-store-listing.md` |

## Step 1: upload the release (about 10 minutes)

1. Play Console, **Test and release → Production → Create new release**.
2. Upload `app-release.aab`. Play should show version 7 (1.1.0).
3. Release notes (paste, English):
   `Shared cut plans now carry a small "Made with StockCut" line and a link, so whoever you send a plan to can find the app. The plan itself is unchanged.`
4. Save, review, **Start rollout to Production**. Use a staged rollout of 20% if you want a safety net; the change is small and Android Vitals was clean on 1.0.5.

## Step 2: replace the screenshots (about 10 minutes)

Play Console, **Grow users → Store presence → Main store listing → Phone screenshots**.

1. Delete the five existing screenshots.
2. Upload from `store/screenshots/captioned/` **in this order**: `01-cut-plan`, `05-fractional-inches`, `02-parts-metric`, `03-stock`, `04-jobs`.

## Step 3: add the three English listings (about 20 minutes)

Paste from the `.txt` files in VS Code, not from a terminal. After pasting, look for `—` and `•`, not `â€`.

1. Same page, top: check the default language. If it is not English (United States), change it under **Manage translations → Change default language**.
2. Paste `store/listing/en-US/short-description.txt` and `full-description.txt` into the default listing.
3. **Manage translations → Add your own translations**, tick English (United Kingdom) and English (India).
4. Paste `en-GB/` and `en-IN/` into their listings. The app name is the same in all three. The five screenshots can be reused.
5. Save and send for review.

## Step 4: set up the share-install counter (5 minutes, once)

Installs from shared plans carry `utm_source=share`. In Play Console, **Grow users → User acquisition** (or Statistics, "Acquisition"), filter or group by UTM source to see them. Data appears after a few days.

## Step 5: start a listing experiment (after Step 3 is live)

**Grow users → Store listing experiments → Create experiment → Main store listing.** Test the first screenshot (or the short description) with the original as control. Leave it running at least two weeks before reading the result.

## Step 6: free promotion (this week)

1. Record a 30-second screen video: a messy parts list in, a 2%-waste plan out.
2. Post it where tradesmen are: r/Welding, r/Fabrication, r/metalworking, r/Carpentry, Facebook fabrication and carpentry groups, YouTube Shorts. Read each group's self-promotion rules first, and say plainly that you built it.
3. Locally: WhatsApp groups of fabricators and carpenters; a printed card with the Play link at a steel or timber yard counter (ask first).

## Step 7: measure for four weeks

In Play Console, **Statistics → Store listing visitors and Conversion rate**, plus the peer comparison. Write down visitors and conversion now, before Step 2 goes live, so you can tell what changed.

- Visitors low: discovery problem. More listing languages, more search words, more posts (Step 6).
- Visitors fine, conversion low: the listing is the problem. Run the experiments in Step 5.

## Not done, and why

- **Translating the app itself (Spanish, Portuguese, German, French).** All the app's text is written directly in the code (59 places, no string resources). Translating means moving every string out first. Adding a listing in a language the app does not speak invites installs followed by uninstalls and low ratings, so do the app first. See the decision in the chat.
- **Price-saved line and offcut library.** Both are new features; worth doing only after the numbers in Step 7 show people come back.
