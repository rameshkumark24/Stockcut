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
| 3 | What were you cutting? | Short answer | ❌ | **The most valuable field in the form** — see §3 |
| 4 | Your email | Short answer | ❌ | Label it: *"Only if you want a reply. Leave blank otherwise."* |
| 5 | Diagnostics | Short answer | ❌ | **Pre-filled by the app.** Tell the user: *"Auto-filled — helps me reproduce it. Clear it if you'd rather not send it."* |

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

## 6. 🔴 Compliance impact — this changes your Play declarations

Adding this feature means **the app now collects data**. Two documents must change or the store listing becomes inaccurate.

### 6.1 Data safety form

| Data type | Collected | Required? | Purpose |
|---|---|---|---|
| Email address | ✅ (field 4) | **Optional** | App functionality — support |
| App version, OS version, device model | ✅ (diagnostics) | **Optional** | App functionality — diagnostics |
| Free-text feedback | ✅ | Optional | App functionality |
| Advertising ID | ✅ *(AdMob, separate)* | — | Advertising |
| Crash logs | ✅ *(Crashlytics, separate)* | — | Diagnostics |

Declare all of it as **optional** and **user-initiated** — which it genuinely is. Mark "data is not shared with third parties" only if that's true; note that Google is the form processor.

### 6.2 Privacy policy

Add a section covering: what the form collects, that it's optional and user-initiated, that Google processes it, that email is only used to reply, and how long you keep responses (**pick a number — 24 months is reasonable**).

### 6.3 The TRD line that is now wrong

[`02-trd.md`](02-trd.md) §11 said *"no personal data collected by us."* That is no longer true if a user types their email. **Corrected in that document.** Flagging it here because an inaccurate data-safety declaration is a real cause of app suspension, and this is exactly how it happens — a small feature added late, declarations never revisited.

---

## 7. Triage routine

A feedback channel nobody reads is worse than none — it makes users feel ignored.

- **Weekly, 15 minutes.** Open the sheet, read everything.
- Tag each: `bug` · `feature` · `wrong-plan` · `confusion` · `noise`
- 🔴 **Any `wrong-plan` response is P0.** Reproduce it that day, and add it to the oracle set in [`06-test-plan.md`](06-test-plan.md) §2.5 as a permanent regression test. **A real user's broken job is the most valuable test case you will ever get** — better than anything you'd invent.
- `confusion` responses are UX bugs, not user error. If two people are confused by the same thing, it's your fault.
- Reply to anyone who left an email. At this volume you can reply to all of them, and early users who get a personal reply become your reviewers.

## 8. Success criteria

| Metric | Target | What it tells you |
|---|---|---|
| Responses per 100 installs | ≥ 2 | The channel is discoverable |
| `wrong-plan` reports | **0** | The optimizer is correct |
| Distinct trades in field 3 | ≥ 5 | Who your users actually are |
| Median reply time | < 7 days | You're actually reading it |

**If `wrong-plan` reports are non-zero, stop all feature work and fix the optimizer.** Correctness is the entire product.

---

## 9. When to replace this

Google Forms is the right tool for **0 to ~500 users**. Move on when either is true:

- More than ~10 responses a week (a spreadsheet stops being triage-able)
- You need a reply thread on a single issue

Then move to a real support inbox or a lightweight tool. **Not before** — it costs money and it's premature.
