# UI/UX Design Brief — StockCut

**Design principle:** this is a tool used with dirty hands, in bad light, standing up. Every decision below follows from that.

---

## 1. Voice and tone

Blunt, workmanlike, no cuteness. The user is a professional; write like a colleague, not a mascot.

| Don't | Do |
|---|---|
| "Oops! Something went wrong 😅" | "Couldn't save. Try again." |
| "Awesome! Your cut plan is ready!" | "7 bars · 4.2% waste" |
| "Get StockCut Pro today!" | "$4.99, one time. Not a subscription." |
| "Please enter a valid length" | "Length must be more than 0." |
| "Loading your amazing projects..." | *(nothing — it's instant)* |

No emoji in product copy. No exclamation marks. Numbers do the talking.

## 2. Colour tokens

Industrial, high-contrast, deliberately not the generic blue every competitor uses.

| Token | Light | Dark | Use |
|---|---|---|---|
| `primary` | `#E8590C` | `#FF8A3D` | Primary actions, Optimize button |
| `onPrimary` | `#FFFFFF` | `#1A0E06` | |
| `surface` | `#F8F9FA` | `#14181C` | Screen background |
| `surfaceContainer` | `#FFFFFF` | `#1E2429` | Cards, sheets |
| `onSurface` | `#14181C` | `#E3E6E8` | Body text |
| `onSurfaceVariant` | `#5A636B` | `#A8B0B8` | Secondary text, helper text |
| `outline` | `#C9CFD4` | `#3A424A` | Dividers, field borders |
| `cutSegment` | `#2D7DD2` | `#4A9BE8` | Cut pieces in the plan bar |
| `cutSegmentAlt` | `#1E5B9A` | `#3579B5` | Alternating segments for adjacency |
| `offcut` | `#9AA3AB` | `#5F6870` | Offcut/waste segment |
| `success` | `#2A9D3F` | `#4FBF63` | Waste < 5% |
| `warning` | `#D48806` | `#F0A92E` | Waste 5–15% |
| `error` | `#C0392B` | `#F0685A` | Waste > 15%, infeasible, destructive |

**Contrast:** every text/background pair ≥ **4.5:1** (WCAG AA). Verify with a contrast checker, not by eye — this app is used in direct sunlight.

**Colour is never the only signal.** Waste % is colour-coded *and* labelled. Cut-plan segments differ in colour *and* carry a text label — a colour-blind user must read the same plan.

## 3. Type scale

| Role | Size / weight | Use |
|---|---|---|
| `displayNumber` | 32sp / SemiBold / **tabular** | The waste % and bar count on the result screen |
| `headline` | 22sp / SemiBold | Screen titles |
| `title` | 16sp / Medium | Card titles, job names |
| `body` | 15sp / Regular | Helper text, descriptions |
| `measurement` | 16sp / Medium / **tabular** | **Every length, everywhere** |
| `label` | 13sp / Medium | Field labels, chips |
| `caption` | 12sp / Regular | Timestamps, secondary meta |

**Non-negotiable: all measurements render in tabular (monospaced) figures** so digits align vertically in lists. `Roboto` with `FontFeature("tnum")`, or `JetBrains Mono` for measurement fields. A column of right-aligned lengths that doesn't line up looks broken to someone who reads numbers for a living.

**Font scaling:** all layouts must survive the system font scale at maximum. No fixed-height rows. Test it — many users are 45+ and run large text.

## 4. Spacing scale

`4 · 8 · 12 · 16 · 24 · 32 · 48` dp. Nothing else.

- Screen horizontal padding: **16 dp**
- Between cards: **12 dp**
- Inside cards: **16 dp**
- Section gap: **24 dp**

## 5. Touch targets — larger than Material defaults

| Element | Size |
|---|---|
| Minimum tappable | **48 dp** (never smaller) |
| Primary buttons | **56 dp** height |
| List row height | **64 dp** minimum |
| Number entry fields | **56 dp** height |
| Icon buttons | **48 dp** with a 24 dp icon |

Rationale: gloves, cold hands, a phone held one-handed while the other holds a tape measure.

## 6. Component inventory

| Component | Notes |
|---|---|
| **`MeasurementField`** | **The critical custom component.** Accepts `1200`, `3/4`, `1 5/16`, `8' 3 1/2"`. Numeric-first keyboard with a `/` and `'` key. Live-formats on blur to the project's unit. Inline error below, never a dialog. Everything else in the app is standard; **this one deserves its own week.** |
| `CutPlanBar` | Horizontal proportional strip, alternating segments, labelled, offcut in grey at the right. This is the app's signature visual and its main store screenshot. |
| `WasteBadge` | Percentage + colour + label |
| `StockRow` / `PartRow` | Length (tabular, right-aligned) · qty · label · swipe-to-delete |
| `QuickAddChips` | Common stock sizes, unit-aware |
| `EmptyState` | Icon + one-line headline + one-line sub + one primary button. Used on 4 screens. |
| `PrimaryButton` / `SecondaryButton` | 56 dp, full-width on sheets |
| `PaywallSheet` | Bottom sheet, 4 bullets, price, buy, restore |
| `SummaryStrip` | Bars · waste % · total offcut |
| `InlineBanner` | Info / warning / error variants |

**Ten components. That is the whole app.** If the inventory grows past fifteen, scope has drifted.

## 7. The four states, per screen

Required by the checklist for every screen. Filled in fully in [`03-app-flow.md`](03-app-flow.md). Summary of the rule set:

- **Loading** — mostly *does not exist*. Local reads are instant. Adding a spinner to a 5 ms operation makes the app feel slower. Only the optimize action may show progress, and only past 300 ms.
- **Empty** — every empty state has a headline, a one-line explanation, and exactly one action.
- **Error** — inline and specific. Never a modal dialog for a validation problem.
- **Success** — the cut plan. Give it the most design attention; it is the payoff and the screenshot.

## 8. Microcopy — write it now, not during build

| Where | Text |
|---|---|
| Kerf field helper | "Your saw blade width. Typical: 3 mm / 1/8"" |
| Trim field helper | "Removed from the start of each length if the end is damaged." |
| Empty parts | "What do you need to cut?" |
| Empty stock | "What lengths are you buying?" |
| Empty projects | "No jobs yet." / "Add your stock lengths and the pieces you need — StockCut works out the cuts." |
| Optimize button | "Optimize" |
| Infeasible error | "2 parts don't fit any stock length." |
| Shortfall | "You need 2 more 6 m lengths to cut everything." |
| Paywall headline | "Unlock unlimited parts" |
| Paywall price | "$4.99, one time. Not a subscription." |
| Purchase success | "Unlocked. Thanks." |
| Delete confirm | "Delete this job?" / "This can't be undone." |
| Validation — zero | "Length must be more than 0." |
| Validation — unparseable | "Try 1200, 3/4, or 1 5/16" |

## 9. Destructive actions

Per checklist: destructive actions get confirmation.

- **Delete part / stock row** → immediate delete + **undo snackbar (5 s)**. No dialog — it's one row and undo is cheaper than a prompt.
- **Delete project** → confirmation dialog. It represents real work.
- **Clear all parts** → confirmation dialog.
- Nothing in this app warrants typed confirmation.

## 10. Motion

Minimal. Motion costs frames on a low-end device.

- Screen transitions: default Material shared-axis
- `CutPlanBar` segments: a single 250 ms staggered fill on first appearance — this is the one moment of delight and it doubles as a demo in the store video
- No parallax, no hero animations, no confetti
- Respect "Remove animations" in system accessibility settings

## 11. Store assets — designed here, not later

ASO is the entire distribution channel (gap audit §B8), so the screenshots are a design deliverable, not an afterthought.

| Asset | Content |
|---|---|
| Screenshot 1 | **The cut plan**, full colour, caption: *"See exactly which pieces come off which bar"* |
| Screenshot 2 | Parts entry with fractional inches visible, caption: *"Type 1 5/16" — no converting to decimals"* |
| Screenshot 3 | Waste summary, caption: *"Cut your offcut waste to under 5%"* |
| Screenshot 4 | Airplane-mode indicator visible, caption: *"Works with no signal"* |
| Screenshot 5 | Shared image in a chat, caption: *"Send the cut list to your apprentice"* |
| Feature graphic | 1024×500 — the `CutPlanBar` visual, app name, one line |

Every screenshot carries a caption. Most installs are decided on screenshots alone.
