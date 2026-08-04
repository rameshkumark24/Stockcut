# App Flow — StockCut

Every screen, every route, every transition — including the error and empty paths, which is where the checklist says most apps are underspecified.

---

## Route map

```
projects                       Projects list (start destination)
project/{id}                   Project editor  — tabs: Parts · Stock · Setup
project/{id}/result            Cut plan
settings                       Global settings
about                          About / support / legal
```

Modal sheets (not routes): `AddPartSheet` · `AddStockSheet` · `UnitPickerSheet` · `PaywallSheet` · `DeleteConfirmDialog`

---

## S1 — Projects list *(start)*

**Purpose:** get an existing job open, or start a new one, in one tap.

| State | What the user sees |
|---|---|
| **Empty** (first run) | Illustration of a cut bar. *"No jobs yet."* Sub: *"Add your stock lengths and the pieces you need — StockCut works out the cuts."* Primary button: **New job**. |
| **Populated** | Cards sorted by last modified: job name · part count · last-modified date · last known waste %. FAB: **New job**. |
| **Free tier at limit** (1 saved project) | FAB still visible; tapping it opens `PaywallSheet` with *"Free plan saves one job. Unlock to save unlimited."* |
| **Loading** | None. Local Room read is instant — do **not** add a spinner. |
| **Error** | Only reachable if the DB is corrupt: *"Couldn't open your saved jobs."* + **Email support** button. |

**Actions:** tap card → `project/{id}` · long-press → Rename / Duplicate / Delete · overflow → Settings, About

---

## S2 — Project editor

Three tabs. **Parts is the default tab** — it is where the work is.

### S2a — Parts tab

| State | What the user sees |
|---|---|
| **Empty** | *"What do you need to cut?"* + **Add part** button, large and centred |
| **Populated** | Rows: `Length · ×Qty · Label`. Right-aligned lengths in tabular figures so columns line up. Header shows `14 / 20 parts` on free tier. Sticky bottom bar: **Optimize**. |
| **At free limit** | Add button opens `PaywallSheet`. Existing parts stay fully editable — **never lock someone out of data they already entered.** |

**Row actions:** tap → edit sheet · swipe → delete (with undo snackbar, 5 s)

### S2b — Stock tab

| State | What the user sees |
|---|---|
| **Empty** | *"What lengths are you buying?"* + **Add stock**. Quick-add chips for common sizes based on the project's unit: metric `3 m · 6 m · 12 m`, imperial `8' · 10' · 12' · 16' · 20'`. |
| **Populated** | Rows: `Length · ×Qty or ∞ · Label` |

**Quantity control:** a number, or a toggle to **Unlimited** ("I'll buy as many as needed"). Unlimited is the default — it is what most users mean.

### S2c — Setup tab

- **Units** — opens `UnitPickerSheet` (mm · cm · m · inch decimal · inch fractional)
- **Fraction denominator** — 1/8 · 1/16 · 1/32 · 1/64 *(shown only when unit = fractional inch)*
- **Kerf** — measurement field, defaults from global settings. Helper text: *"Your saw blade width. Typical: 3 mm / 1/8""*
- **End trim** — optional, defaults 0. Helper: *"Removed from the start of each length if the end is damaged."*
- **Job name** — text field

**Changing units** re-formats every displayed measurement. **It never changes the stored value** — internal `Long` is unit-agnostic. This must be verified by test (`03` in the test plan).

---

## S3 — Optimize transition

Triggered by the **Optimize** button on any tab.

```
tap Optimize
   ├─ validate ──▶ any part longer than longest stock?  ──▶ S3-ERR-1
   ├─ validate ──▶ no parts, or no stock?               ──▶ inline toast, stay
   ├─ ads: free tier & 3rd optimize? ─▶ interstitial ─▶ continue
   └─ run optimizer (background dispatcher)
          ├─ < 300 ms ──▶ navigate straight to S4 (no spinner — a flash of spinner is worse than none)
          └─ ≥ 300 ms ──▶ inline progress on the button, then S4
```

### S3-ERR-1 — Infeasible parts
Full-width red banner **before** navigating:
> **2 parts don't fit any stock length.**
> `2400 mm` and `2100 mm` are longer than your longest stock (`2000 mm`).
> [Add longer stock] [Edit those parts]

**Never navigate to a plan that silently omitted parts.** This is the single most damaging possible bug — the user cuts to a plan and discovers at the end that two pieces were never included.

---

## S4 — Cut plan

The screenshot that sells the app. Design it first, not last.

**Layout, top to bottom:**

1. **Summary strip** — `7 bars · 4.2% waste · 336 mm offcut total`. Waste % is colour-coded: green < 5%, amber 5–15%, red > 15%.
2. **Shortfall banner** *(only if `Shortfall`)* — *"You need 2 more 6 m lengths to cut everything."*
3. **Bar list** — one card per stock bar:
   - Horizontal strip divided into proportional segments, each labelled with its length; offcut segment in grey at the right end
   - Text beneath: `Bar 3 — 1200 · 1200 · 850 · 400 → offcut 336`
   - Identical bars are **collapsed**: *"×4 identical bars"* — a plan with 40 bars must not be 40 cards
4. **Action row** — **Share image** · **Export PDF** (paid; free tier shows a lock and opens `PaywallSheet`)

| State | What the user sees |
|---|---|
| **Success** | The above |
| **Shortfall** | The above + banner (2). The plan is still shown and still useful. |
| **Empty** | Unreachable — S3 validation blocks it |
| **Error** | Unreachable — the optimizer returns results, it does not throw |

**Back** returns to the editor with the plan cached, so re-entering doesn't recompute.

**In-app review prompt:** fires here, only after a *successful* optimize, only if ≥ 3 lifetime optimizes, at most once per 90 days. Never on launch.

---

## S5 — Paywall sheet

Reached from: free-tier part limit · second project · PDF export · About screen.

**Content:**
- Headline naming what they just hit: *"Unlock unlimited parts"* — not a generic "Go Pro"
- Four bullets: unlimited parts · unlimited jobs · PDF export · no ads
- **$4.99, one time. Not a subscription.** — state this explicitly; it is a conversion driver
- Buy button · **Restore purchases** · dismiss

| State | Behaviour |
|---|---|
| Purchase success | Sheet closes, snackbar *"Unlocked. Thanks."*, ads disappear immediately, the blocked action proceeds |
| Purchase cancelled | Sheet closes silently. **No nag, no second prompt.** |
| Billing unavailable | *"Can't reach Google Play right now."* + Retry. The app stays fully usable on the free tier. |
| Already owned | Auto-detected on launch; the sheet never appears |

---

## S6 — Settings

Global defaults applied to *new* projects only — changing them never mutates existing projects.

- Default unit · default fraction denominator · default kerf
- Theme: System / Light / Dark
- **Restore purchases**
- Privacy settings (UMP consent form re-entry — required to be reachable for EU users)

---

## S7 — About / support

- Version + build number *(support asks for this first)*
- **Email support** — pre-fills device model, Android version, app version in the body
- Privacy policy (opens browser)
- Rate on Play
- Unlock / restore
- Open-source licences

---

## Cross-cutting states

| Situation | Behaviour |
|---|---|
| **Airplane mode** | Everything works except buying and ads. No error dialogs, no "you're offline" banner — offline is the normal condition, not an exception. |
| **Process death** (backgrounded, killed) | In-progress edits already persisted to Room; return to the same project and tab |
| **Rotation** | State preserved via `ViewModel`; no recompute |
| **Max font scale** | All layouts scroll; no clipping; no fixed-height rows |
| **Dark mode** | Full support; the cut-plan bar segments must stay distinguishable |
| **Very long job name** | Ellipsised in lists, wrapped on detail |
| **Ad fails to load** | Container collapses to zero height. No blank grey box. |

---

## First-run experience

The checklist calls the empty state the most-seen and least-designed screen. Here it is designed:

**On first launch, do not show an onboarding carousel.** Instead, seed a **read-only example job** — "Example: gate frame" — with 4 stock lengths and 9 parts, already optimized. The user taps it, sees a finished cut plan in 3 seconds, and understands the entire product without reading a word.

A **Delete example** action is on the card. It never reappears once deleted.
