# StockCut is free — the decision, and what would have to change to undo it

**Decided 2026-08-08.** This supersedes the monetisation section of `docs/02` §6.
The code that implements it is
[`Monetization`](../data/src/main/kotlin/com/stockcut/data/entitlement/Entitlement.kt).

---

## The decision

**StockCut is completely free. The only revenue is AdMob.**

No in-app purchase, no paywall, no price, and nothing anywhere in the UI that
offers to sell anything. It publishes on the owner's own Play Console account.

## Why

A brand-new app from an unknown developer converts close to nothing on a paid
unlock. Nobody pays $4.99 on sight for a tool with no reviews, no reputation and
twelve downloads. The unlock would have sat there earning approximately zero
while making every screenshot and every review mention a price.

Ads earn less per user, but they earn from **every** user — including the large
majority who would never have paid — and they cost nothing to collect. A free
tool also spreads: a tradesman shows it to the next one on site, which is this
app's only real distribution channel.

### What this costs, honestly

**The app learns nothing about willingness to pay.** Whether $4.99 is right,
whether $2.99 converts better, whether anyone pays at all — none of that is
knowable while there is no purchase button. That was accepted deliberately.

**Expect very little ad revenue at first.** AdMob does not pay out below $100,
and a niche trade utility launched cold, with no marketing, may take a long time
to reach it. The job of v1 is users, reviews, and finding out whether tradesmen
reach for this on a real job. The money, if it comes, comes from knowing that.

---

## 🔴 The one thing that had to ship from day one

**`Settings.firstRunAt`** — the date this user first ran the app.

Nothing reads it today. It exists so that *if* a paywall is ever added, everyone
already using the app can keep what they have. Silently taking features back from
existing users is the surest way to turn a working app into one-star reviews, and
it would land at exactly the moment there are finally users worth keeping.

Answering *"was this person here before the cutoff?"* is **impossible unless the
app wrote it down at the time**. It cannot be added alongside the paywall that
needs it. That is why a value nothing currently reads is recorded on every first
launch.

It never leaves the device. It is not analytics.

---

## If the paywall is ever turned on

There is no transfer step and no dependency on anyone else — the app is on the
owner's own account from the start. So this is purely a product decision, made
whenever the data justifies it.

1. Set `Monetization.PAYWALL_ENABLED = true`.
2. **Add the grandfather rule.** Not optional, and not implemented — what ships is
   the hook, not the rule. Users whose `firstRunAt` precedes the cutoff are
   entitled permanently, without paying and without a Play lookup (there is no
   server, and they never bought anything for Play to confirm).
3. Create the product in Play Console: `stockcut_unlock`, one-time,
   non-consumable. The ID is already in `BillingManager.UNLOCK_PRODUCT_ID` and is
   **permanent once created**.
4. Set up a **payments profile** — bank details and tax information. In India that
   also means deciding the GST/export-of-service treatment of foreign app revenue,
   which is worth a CA's time before the first payout, not after.
5. Add license testers and **walk the full billing matrix** — purchase,
   acknowledge, restore, refund, pending, offline. 🔴 None of it has ever run
   against real Play; it is written and unit-tested only.
6. Update the Play **data safety form** and the store listing: the app now
   contains in-app purchases.
7. Re-enable the Settings "Purchase" section and "Restore purchases" — both are
   already written, behind the same flag.

---

## What is deliberately still in the codebase

`BillingManager`, `PaywallSheet`, `PaywallHost`, every `PaywallTrigger`, the tier
gates, and the Settings purchase section are **all still present and still
compiled**, behind the flag.

They were not deleted because unpicking billing across four screens and rewriting
it later is far more likely to go wrong than flipping one constant. The tests
reflect this:
[`PaywallRulesTest`](../data/src/test/kotlin/com/stockcut/data/EntitlementTest.kt)
exercises every dormant rule with `paywallEnabled = true` on **every build**, so
the paywall cannot quietly rot while switched off.

`EntitlementTest` asserts `PAYWALL_ENABLED == false` outright — so switching it on
fails the build loudly, with a message pointing here. That is intentional. It
should be a deliberate act, never a quiet one.

### The BILLING permission is still in the manifest

`com.android.vending.BILLING` is merged in by the Play Billing library and has
been left there. It is unused and declaring it is not a policy problem — Play
derives the listing's "In-app purchases" badge from the products that actually
exist in the console, and there are none.

It was left rather than stripped with `tools:node="remove"` for the same reason as
everything else here: removing it creates a step that must be remembered and
reversed later, in exchange for tidying a line nobody reads.

**Declare "In-app purchases: No" on the listing regardless.** That answer is about
products, and it is correct.
