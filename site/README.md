# `site/` — the two pages that must be publicly reachable

Both are plain HTML with no build step. Publish them on **GitHub Pages**, which
is free and needs no domain.

## What each one is for

| File | Purpose | Where it is used |
|---|---|---|
| `privacy-policy.html` | Required by Play because AdMob and Crashlytics collect data | Play Console listing, and the About screen |
| `feedback.html` | 🔴 The redirect that stands between the app and the Google Form | The About screen, and "This plan looks wrong" on the cut plan |

## Publish them

1. Settings → Pages → Source: **Deploy from a branch**, branch `main`,
   folder `/site` *(or copy both files into a `docs/` folder if you prefer that
   convention)*.
2. Wait a minute, then confirm both URLs load:
   - `https://<user>.github.io/Stockcut/privacy-policy.html`
   - `https://<user>.github.io/Stockcut/feedback.html`

## Before publishing — fill in the blanks

**`privacy-policy.html`** has two placeholders, both marked in the file:
- `[DATE]`
- `[YOUR SUPPORT EMAIL]` — appears twice

**`feedback.html`** has one:
- `FORM_URL` — the Google Form's `/viewform` address, in the `<script>` block

## 🔴 Why the redirect exists

The app links to `feedback.html`, **never to the Google Form directly.**

This app has no server, so there is normally no way to change anything without
shipping an update and waiting days for store review. That one layer of
indirection is the only exception: if the form is flooded, needs restructuring,
or you outgrow Google Forms entirely, you edit one line here and it is live in a
minute.

Retrofitting it later costs exactly the app update it exists to avoid, which is
why `docs/09` §9.5 says to set it up **before** the URL ships.

The page carries `window.location.search` through to the form. **Do not remove
that** — the app appends the pre-filled diagnostics as a query parameter, and
without it every report arrives missing the app version, device and unit mode
that make it reproducible.

## The Google Form itself

I cannot create this — it needs your Google account. Four fields, no more
(`docs/09` §2):

| # | Field | Type | Required |
|---|---|---|---|
| 1 | What's this about? | Multiple choice: `Something's broken` · `Idea for a feature` · `The cut plan was wrong` · `Something else` | ✅ |
| 2 | What did you expect, and what happened instead? If it's a wrong cut plan, include the stock length and the pieces. | Paragraph | ✅ |
| 3 | What were you cutting? | Short answer | ❌ |
| 4 | Diagnostics | Short answer | ❌ |

Field 2's wording is doing real work: responses are anonymous so you **cannot
ask a follow-up**, which means the prompt has to extract the detail up front.
Set a minimum length on it (Response validation) to kill one-word submissions.

Field 3 is the most valuable one in the form — it tells you whether the
metal-trades bet in `docs/00-phase-0` was right.

🔴 **No email, name, or contact field.** Not an oversight — it is what keeps the
Play data-safety declaration to categories Crashlytics already forces. Adding one
means updating the data safety form and the privacy policy in the same change
(`CLAUDE.md` rule 11).

Then: Responses → ⋮ → **Get email notifications for new responses**.

### Getting the entry ID the app needs

1. Form → ⋮ → **Get pre-filled link**
2. Type `DIAG` into the Diagnostics field → **Get link**
3. The URL contains `entry.123456789=DIAG`. Send me that number and the
   `feedback.html` URL, and the two missing links in About get wired up.
