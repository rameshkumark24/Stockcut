# Feedback Channel — Google Form

**Purpose:** let real users report bugs and request features directly, and get an email the moment they do.

**Why this is the right call at this stage:** zero backend, zero cost, zero maintenance, and it fits the ₹0/month constraint exactly. Google Forms gives you a spreadsheet, email notifications, and structured responses without a line of server code.

**Why it beats Play Store reviews as a feedback channel:** reviews are public, one-way, capped at a star rating, and a bug report there costs you a star. A form is private, structured, and the user isn't punishing you to use it.

---

## 1. What it is *not* for

- **Not crash reports.** Crashlytics captures those automatically, with stack traces. A user typing "it crashed" is worth less than a stack trace you already have.
- **Not support tickets** that need a reply thread. Email is better for that.
- **Not a substitute for talking to your tradesman contact.** One 10-minute phone call beats 20 form responses.

---

## 2. Form design

**Keep it under 60 seconds to fill.** Every extra field halves your response rate.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | What's this about? | Multiple choice | ✅ | `Something's broken` · `Idea for a feature` · `The cut plan was wrong` · `Something else` |
| 2 | Tell me what happened | Paragraph | ✅ | |
| 3 | What were you cutting? | Short answer | ❌ | **The most valuable field in the form** — see §2.1 |
| 4 | Diagnostics | Short answer | ❌ | **Pre-filled by the app.** Tell the user: *"Auto-filled — helps me reproduce it. Clear it if you'd rather not send it."* |

> 🔴 **No email field. No name field. No contact field of any kind.** This is a deliberate decision, not an omission — see §6. Do not add one without redoing the data-safety declaration.

**Where replies happen instead:** the About screen keeps a separate **"Email support"** `mailto:` link. That gives a clean split:
- **Form** → structured report, anonymous, no reply expected
- **`mailto:`** → "I want an answer", and the user hands you their address themselves, through their own mail app

This is a better design than one channel doing both, and it is why removing the email field costs nothing.

**"The cut plan was wrong" gets its own option** on purpose. That is a correctness bug in the one thing this app must get right, and you want it flagged, not buried in "something else."

### 2.1 Why field 3 matters more than the rest

At this stage you have a hypothesis: *the underserved user is a metal fabricator or framer cutting linear stock, not a cabinetmaker.* Field 3 is how you find out if that's true.

If 40 responses come back saying "kitchen carcasses, 18 mm MDF," your wedge is wrong and 2D panel cutting just became v1.1 instead of v2. That single question can redirect the entire product. Ask it.

---

## 3. Email notification setup

**Simple path (start here):**
Form → **Responses** tab → ⋮ → **Get email notifications for new responses**

That's it. No Apps Script, no code. Notifications go to the form owner's Google account.

**Richer path (only if you want the response text in the email body, or a second recipient):**
Responses → link to a Google Sheet → Extensions → Apps Script → `onFormSubmit` trigger → `MailApp.sendEmail(...)`.

Do the simple path first. Upgrade only if the notifications aren't useful enough.

> Set up the form and the notification **in W0**, before any code. Then the app just needs a URL.

---

## 4. Pre-fill mechanics

Google Forms supports pre-filling any field via URL query parameters.

**One-time setup:**
1. Open the form → ⋮ → **Get pre-filled link**
2. Type a dummy value into the Diagnostics field (e.g. `DIAG`)
3. **Get link** → copy it
4. The URL contains `entry.123456789=DIAG` — that number is the field's entry ID
5. Record the form ID and entry ID in `local.properties`, injected via `buildConfigField`

**At runtime:**

```
https://docs.google.com/forms/d/e/{FORM_ID}/viewform
  ?usp=pp_url
  &entry.{DIAG_ID}={urlEncoded(diagnostics)}
  &entry.{TYPE_ID}={preselected type, when launched from a specific context}
```

### 4.1 The diagnostics string

**One field, not six.** A single compact string is easier for the user to read and decide about, and easier for you to scan in a spreadsheet.

```
v1.0.3 (12) | Android 13 | SM-A515F | mm 1/16 | free | 47 optimizes
```

| Part | Why it's there |
|---|---|
| App version + build | First question you'd ask anyway |
| Android version | Reproduces OS-specific bugs |
| Device model | Low-end device issues cluster by model |
| Unit system + denominator | **Fractional-inch bugs are the predicted failure mode** — you need to know which mode they were in |
| Free / paid | Tells you if a paying user is unhappy |
| Lifetime optimize count | Distinguishes a first-run confusion from a power user hitting a real limit |

**Nothing here identifies a person.** No advertising ID, no install ID, no location, no project contents. Do not add them.

### 4.2 Transparency is the design, not a compromise

The diagnostics field is **visible and editable** in the form before the user submits. They can read it and clear it.

This is deliberate: it's honest, it builds trust with a professional audience, and it makes the Play data-safety declaration straightforward — the user sees exactly what they're sending and chooses to send it.

---

## 5. Implementation

### 5.1 Opening the form

```
Intent(Intent.ACTION_VIEW, uri) → external browser
```

**No WebView** — banned in `CLAUDE.md`, and a Google Form in a WebView is a poor experience and a login mess.

Chrome Custom Tabs would look nicer but costs an `androidx.browser` dependency. **Not worth it for v1** under the TRD's dependency policy. Revisit if feedback volume justifies it.

### 5.2 Offline fallback

The form needs a network. The app is used in workshops with no signal, so this **will** happen.

```
if (no network available)
    → show a sheet: "You're offline. Send it as an email instead?"
    → Intent.ACTION_SENDTO with mailto:
      subject: "StockCut feedback"
      body:    "\n\n---\n{diagnostics}"
    → the mail app queues it and sends when signal returns
```

Offer both paths in the About screen regardless, so a user with no browser or no mail app always has one that works.

### 5.3 Entry points

| Location | Label | Notes |
|---|---|---|
| **S7 About** | "Report a problem or suggest a feature" | Primary, always available |
| **S4 Cut plan** overflow | "This plan looks wrong" | Pre-selects form field 1 = *The cut plan was wrong* |

**Two entry points. Not more.**

- ❌ Never behind the paywall — a free user's bug report is worth as much as a paid one
- ❌ Never a nag, popup, or interstitial prompt
- ❌ Never triggered after a crash — Crashlytics has it, and asking someone to fill a form right after your app crashed is insulting

The "This plan looks wrong" entry is worth its weight: it catches the one bug class that would otherwise arrive as a 1-star review saying *"wasted my steel."*

---

## 6. Compliance impact — deliberately kept near zero

**Design decision: the form collects no personal data.** No email, no name, no contact field, no advertising ID, no install ID, no location, no project contents.

What it does collect is app version, OS version, device model, unit mode, tier, and optimize count — none of which identifies a person, and all of which the user sees on screen and can delete before sending.

### 6.1 Data safety form

| Data type | Source | Category | Notes |
|---|---|---|---|
| Crash logs, device state | **Crashlytics** | Diagnostics | Already declared — automatic |
| Advertising ID | **AdMob** | Advertising | Already declared — per UMP consent |
| App/OS/device version, unit mode, tier | Feedback form | Diagnostics | **Same category Crashlytics already forces** |
| Free-text feedback | Feedback form | User-initiated support content | Optional |

> **The practical effect: with no email field, the form adds no new data-safety category.** Crashlytics already puts *Diagnostics* on your declaration. The form rides on a box you were ticking anyway.

Declare everything from the form as **optional** and **user-initiated** — which it genuinely is. Google is the processor for form responses.

**Confirm the exact categories against the guidance shown inside the Data safety form when you fill it in.** The taxonomy is Google's and it changes; the above is how it maps today, not a quotation.

### 6.2 Privacy policy

One short paragraph: what the form collects, that it is optional and user-initiated, that Google processes responses, and how long you keep them (**pick a number — 24 months is reasonable**).

### 6.3 The rule that outlives this document

An inaccurate data-safety declaration is a real cause of app suspension, and it nearly always arrives the same way: **a small feature added after the declarations were written, and nobody goes back.**

So: **if you ever add a contact field to this form, the data-safety form and the privacy policy change in the same PR.** That is `CLAUDE.md` rule 11, and it exists specifically to catch future-you.

---

## 7. Triage routine

A feedback channel nobody reads is worse than none — it makes users feel ignored.

- **Weekly, 15 minutes.** Open the sheet, read everything.
- Tag each: `bug` · `feature` · `wrong-plan` · `confusion` · `noise`
- 🔴 **Any `wrong-plan` response is P0.** Reproduce it that day, and add it to the oracle set in [`06-test-plan.md`](06-test-plan.md) §2.5 as a permanent regression test. **A real user's broken job is the most valuable test case you will ever get** — better than anything you'd invent.
- `confusion` responses are UX bugs, not user error. If two people are confused by the same thing, it's your fault.
- **Responses are anonymous — you cannot reply.** That is the trade for collecting nothing. Anyone who wants an answer uses the `mailto:` support link instead, and those you reply to individually.
- Because you can't ask a follow-up, **field 2's prompt has to do that work up front**. Word it to invite specifics: *"What did you expect, and what happened instead? If it's a wrong cut plan, include the stock length and the pieces."*

## 8. Success criteria

| Metric | Target | What it tells you |
|---|---|---|
| Responses per 100 installs | ≥ 2 | The channel is discoverable |
| `wrong-plan` reports | **0** | The optimizer is correct |
| Distinct trades in field 3 | ≥ 5 | Who your users actually are |
| Reports with enough detail to reproduce | ≥ 60% | Field 2's prompt is working — no follow-up is possible, so this is the quality gate |

**If `wrong-plan` reports are non-zero, stop all feature work and fix the optimizer.** Correctness is the entire product.

---

## 9. Abuse handling

### 9.1 Assume the form URL is public

The URL ships inside the APK. Any APK can be decompiled in minutes. **Treat the form link as public information** — never put anything secret in it, and design assuming a stranger can submit to it directly without the app.

### 9.2 What a flood actually costs you

| Server-backed app | StockCut |
|---|---|
| Flood → CPU, bandwidth, DB writes → **a bill** | **₹0.** Nothing is metered. |
| Flood → other users get a slow or down service | **Nobody else is affected.** No shared resource. |
| Flood → possible data corruption | Nothing to corrupt |
| Needs rate limiting, WAF, captcha | Not applicable |

**The only thing a flood can damage is your inbox** — and through it, your willingness to read real feedback. That is the entire threat. Size the response to match; do not build defences for damage that cannot occur.

### 9.3 Legitimate repeat reports — do not block these

A real user sending three reports in one session is a **good** outcome, not abuse. Never block it.

**Soft client-side cooldown only:**
- Max **3 form opens per 24 h**, counted in DataStore
- On the 4th: *"You've sent a few already — I'll read them. Something urgent? Email me."* → offer `mailto:`
- **Never a hard block.** Never a lockout.

This is trivially bypassable and that's fine — its job is preventing accidental double-submits and casual mashing, which is what ~99% of real duplicates are. It is not a security control and should not be described as one.

### 9.4 Field-level hygiene

- **Minimum length on field 2** (Forms → response validation) — kills empty and single-character junk
- Keep field 1 a multiple choice, not free text — makes triage sortable even under noise

### 9.5 🔴 The kill switch you'd otherwise not have

With no server, there is normally **no way to change anything without shipping an app update** — and store review takes days. If the form URL is flooded or abused, you'd be stuck.

**Fix it for free with one layer of indirection:**

```
App links to:   https://rameshkumark24.github.io/stockcut/feedback
                          ↓ (HTML meta-refresh or JS redirect)
Which points to: https://docs.google.com/forms/d/e/{FORM_ID}/viewform?...
```

GitHub Pages is already hosting the privacy policy, so this costs nothing. Now:

- Form flooded? Create a **new form**, edit one line on the redirect page. Live in a minute. No app update, no store review.
- Need to change the form structure? Same.
- Want to retire the form entirely at 500 users (§10)? Point the redirect at whatever replaces it.

> **Set the redirect up in W0, before the URL ships.** Retrofitting it requires the app update you were trying to avoid.

The redirect page must preserve query parameters so the pre-filled diagnostics still arrive.

### 9.6 ⚠️ Do not open form exports in Excel carelessly

Classic **CSV injection**: a response beginning with `=`, `+`, `-` or `@` can be interpreted as a formula when the export is opened in a spreadsheet — including formulas that fetch remote URLs.

- Read responses **in the Google Sheet**, not in a downloaded CSV opened in Excel
- If you must export, open with the columns set to plain text
- **Never click a link inside a response.** You have no reply channel, so there is never a reason to.

### 9.7 What the form cannot be used to do

Worth stating so effort goes to real risks:

- ❌ Cannot reach your app, your users, or their data — there is no connection between a form response and any device
- ❌ Cannot cost you money — nothing is metered
- ❌ Cannot leak personal data — **none is collected** (§6)
- ❌ Cannot cause downtime — there is nothing to take down
- ❌ Cannot be used for account takeover — there are no accounts

**The strongest security control in this app is the data you decided not to collect.**

---

## 10. When to replace this

Google Forms is the right tool for **0 to ~500 users**. Move on when either is true:

- More than ~10 responses a week (a spreadsheet stops being triage-able)
- You need a reply thread on a single issue

Then move to a real support inbox or a lightweight tool. **Not before** — it costs money and it's premature.
