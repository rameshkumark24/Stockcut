# PRD — StockCut

**Product:** 1D linear cut-list optimizer for Android
**Version:** v1.0 (first Play Store release)
**Date:** 2026-08-04 · **Owner:** Rameshkumar
**Upstream:** [`00-phase-0-scope-feasibility.md`](00-phase-0-scope-feasibility.md) · [`00-gap-audit.md`](00-gap-audit.md)

---

## 1. Problem

A tradesman buys material in fixed stock lengths — 6 m steel tube, 16 ft studs, 5 m rebar, 3 m extrusion — and needs a set of shorter pieces cut from it. Working out which pieces come off which bar is a bin-packing problem, and it is done in the head or on the back of a delivery note.

The result is **10–25% waste**. On a job with £2,000 of steel, that is £200–500 thrown in the offcut bin. Every job. Forever.

The existing tools that solve this are either on iOS, in a browser (needs signal and a keyboard), or built for cabinetmakers cutting plywood — not for someone cutting linear stock next to a chop saw.

## 2. Product statement

> **StockCut turns a list of stock lengths and a list of required pieces into a cutting plan with the least waste — accounting for saw kerf — in under 60 seconds, on a phone, with no internet, in fractional inches or millimetres.**

## 3. Personas

### P1 — "Dave", metal fabricator (primary)
- 42, self-employed, two-man shop, UK or US
- Cuts 50×50 SHS, angle iron, tube. Buys in 6 m or 20 ft lengths.
- Phone: mid-range Android, 3 years old, cracked screen, in a pocket with metal filings
- Works in **mm** (UK) — the US equivalent works in **fractional inches**
- Signal in the workshop: unreliable
- **Will pay $5 without thinking** if it saves one length of steel
- Does not want features. Wants an answer.

### P2 — "Mike", timber framer / deck builder (primary)
- 35, US, works in **feet and fractional inches** (`8' 3 1/2"`)
- Buys 8/10/12/16 ft studs and joists
- Needs to hand the cut list to an apprentice — **the plan must leave the phone**

### P3 — "Raj", hobbyist woodworker (secondary)
- Buys the app less readily, reviews it more readily
- Drives ratings, which drive ASO. Do not design *for* him, but do not break things for him.

**Explicitly not a persona:** cabinet makers cutting sheet goods. That is 2D, that is the crowded market, and it is v2 at the earliest.

## 4. User stories

Ordered by priority. Every P0 is in v1; nothing else is.

| ID | Priority | Story |
|---|---|---|
| US-01 | P0 | As Dave, I enter the stock lengths I have available, so the plan uses what I actually own. |
| US-02 | P0 | As Dave, I enter the pieces I need with quantities, so I don't have to type the same length ten times. |
| US-03 | P0 | As Dave, I set my saw blade width once, so the plan is physically cuttable. |
| US-04 | P0 | As Mike, I enter measurements as `3 1/2"`, so I don't have to convert to decimals in my head. |
| US-05 | P0 | As Dave, I tap Optimize and see, per bar, exactly which pieces to cut and in what order. |
| US-06 | P0 | As Dave, I see the total waste and how many bars I need, so I know what to buy. |
| US-07 | P0 | As Dave, I am told clearly when a piece is too long for any stock I have, instead of it silently vanishing. |
| US-08 | P0 | As Dave, I save a job and come back to it next week. |
| US-09 | P0 | As Mike, I share the plan as an image on WhatsApp so my apprentice can cut it. |
| US-10 | P0 | As Dave, I use the app with no signal in a workshop. |
| US-11 | P0 | As Dave, I pay once to remove ads and the limits, and it stays unlocked on a new phone. |
| US-12 | P1 | As Dave, I export a PDF to print and pin to the wall. |
| US-13 | P1 | As Dave, I set a trim allowance because the end of the bar is damaged. |
| US-14 | P1 | As Dave, I save my usual stock sizes so I don't re-enter them each job. |
| US-15 | P2 | As Dave, I duplicate last month's job and change two numbers. |
| US-16 | P0 | As Dave, when the cut plan looks wrong, I can tell the developer in two taps — because a wrong plan costs me steel. |
| US-17 | P1 | As Dave, I can suggest a feature without leaving a public review. |
| US-18 | P0 | As the developer, I get an email the moment someone reports a problem, so early users don't feel ignored. |

## 5. Features — v1 scope

### 5.1 Project management
- Create, rename, duplicate, delete a project
- Projects list sorted by last-modified
- All data local; no account, ever

### 5.2 Stock input
- Add stock entries: **length**, **quantity** (a number, or *unlimited*)
- Multiple different stock lengths per project
- Optional label ("50×50 SHS")
- Edit / delete / reorder

### 5.3 Parts input
- Add parts: **length**, **quantity**, optional **label**
- Edit / delete
- Running count of total parts shown against the free-tier limit

### 5.4 Measurement system
- Per-project unit: **mm · cm · m · decimal inch · fractional inch**
- Fractional inch entry supports `3/4`, `1 5/16`, `8' 3 1/2"`
- Fraction denominator setting: 1/8 · 1/16 · 1/32 · 1/64
- Display always matches the project's chosen unit

### 5.5 Cutting settings
- **Kerf** (saw blade width) — per project, defaults from global settings
- **End trim** — optional allowance removed from the start of each stock length (P1)

### 5.6 Optimize
- Single primary action
- Produces: bars used (per stock size), cut sequence per bar, offcut per bar, total waste %, total offcut length
- Reports **infeasible parts** (longer than any stock) explicitly, and **shortfall** (not enough limited stock) explicitly
- Must complete in **< 2 s for 200 parts** on a low-end device

### 5.7 Cut plan output
- Visual: each bar drawn as a horizontal strip, segments labelled with length, offcut in a distinct colour at the end
- Text list beneath: `Bar 3 of 7 — cut 1200, 1200, 850, 400 → offcut 336`
- **Share as image** (WhatsApp)
- **Export PDF** (paid) — must be legible printed in black and white

### 5.8 Monetisation
- **Free:** up to 20 parts per project, 1 saved project, banner ad + interstitial after every 3rd optimize, no PDF
- **Paid — one-time $4.99:** unlimited parts (hard cap 1000 for performance), unlimited projects, PDF export, no ads
- **Restore purchases** always available
- Paid entitlement **cached locally** and works offline

### 5.9 Support surfaces
- In-app "Email support" link
- Privacy policy link
- In-app review prompt — triggered **only after a successful optimize**, never on launch, never more than once per 90 days

### 5.10 Feedback channel — Google Form
Full spec: [`09-feedback-channel.md`](09-feedback-channel.md)

- **"Report a problem or suggest a feature"** in About → opens a Google Form in the browser
- **"This plan looks wrong"** in the cut-plan overflow → same form, pre-tagged as a correctness bug
- Diagnostics (app/OS/device version, unit mode, tier) pre-filled and **visible to the user before sending**
- **No email, name, or contact field** — the form is anonymous and collects no personal data
- Separate **"Email support"** `mailto:` link in About for anyone who wants a reply
- `mailto:` fallback when offline
- Developer gets an **email notification** on every response
- Never behind the paywall, never nagged, never triggered after a crash

**Why it's P0 for a first app:** at 50 users you cannot A/B test or read analytics. A handful of real tradesmen telling you what's broken is the only signal you'll have — and the "What were you cutting?" field tells you whether the metal-trades wedge in Phase 0 was the right bet.

## 6. Out of scope for v1 — the "later" list

Written down so it stops nagging. None of this ships in 1.0.

2D panel / guillotine cutting · grain direction · edge banding · offcut inventory ("use my leftovers first") · cost per length and job costing · CSV / spreadsheet import · label printing · multiple materials in one project · cloud sync · team sharing · accounts · iOS · web version · subscriptions · analytics SDK · widgets · tablet-optimised layout · languages other than English.

**Strongest v2 candidate:** offcut inventory. Tradesmen have a rack of leftovers and want the plan to consume those first. Nobody does this well. Park it.

## 7. Success metrics

**Primary (two only):**
1. **30-day return rate ≥ 30%** — of users who complete one optimize, how many return within 30 days for a second job
2. **Free → paid conversion ≥ 2%**

**Guardrail:** Play Store rating **≥ 4.3** with ≥ 25 ratings by month 3. Below 4.0, ASO collapses and the other two stop mattering.

**Health (watch, don't target):** crash-free rate ≥ 99.5%, ANR rate below the Play Console bad-behaviour threshold.

**Deliberately not tracked:** total downloads, DAU, session length. This is a tool. A *short* session that solves the problem is the win.

## 8. Non-goals

- Not a CAD tool
- Not an estimating or quoting tool
- Not a project manager
- Not a 2D nesting tool (v2 at the earliest)
- Not a community or content product — no server, no user data, no moderation. **This is deliberate and permanent for v1.**

## 9. Constraints inherited from Phase 0

| Constraint | Value |
|---|---|
| Platform | Android only |
| `targetSdk` | **36** (mandatory for new apps after Aug 31, 2026) |
| `minSdk` | 26 (Android 8.0) |
| Backend | **None.** No network permission except ads. |
| Recurring cost | **₹0** |
| Total investment | ₹2,500 (Play Console $25) |
| Build time | 6 weeks + 3 weeks closed test |
| Primary market | US / UK / CA / AU |

## 10. Open items blocking build

1. ⚠️ **Name one real tradesman** to validate against — blocks the paper test and the closed-test recruiting
2. ⚠️ **Final app name + package name** — permanent once published
