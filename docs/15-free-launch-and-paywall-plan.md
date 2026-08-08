# v1 ships free — the decision, and how the paywall comes back

**Decided 2026-08-08.** This supersedes the monetisation section of `docs/02`
§6 for v1. The code that implements it is
[`Monetization`](../data/src/main/kotlin/com/stockcut/data/entitlement/Entitlement.kt).

---

## The decision

**StockCut v1 is completely free. The only revenue is AdMob.**

There is no in-app purchase, no paywall, no price, and nothing in the UI that
offers to sell anything.

## Why

The owner does not have the $25 for a Play Console account, so v1 publishes on a
friend's account. That account already has **production access** — the friend has
shipped two apps and cleared the 12-tester / 14-day closed test — and production
access is granted **per account, not per app**, so v1 skips the closed test
entirely.

The problem is money. **Play Billing pays the account holder**, always. There is
no sub-account, no split, and no way to route in-app purchase revenue to a third
party. Shipping the $4.99 unlock would have meant:

- this app's largest revenue line landing in someone else's bank account
- that income appearing on someone else's tax record
- the owner depending on a promise to get it back
- refunds and chargebacks hitting the friend's balance

**AdMob has none of these problems.** Ad unit IDs are compiled into the APK and
AdMob pays whoever owns the AdMob account, regardless of who published the app.
The owner's AdMob account is paid directly from day one.

Turning the paywall off removes the whole problem rather than managing it.

### What this costs

Honestly: **six months of pricing data.** With no purchase button, the app learns
nothing about willingness to pay — not whether $4.99 is right, not whether $2.99
converts better, not whether anyone pays at all. That is a real cost and it was
accepted deliberately in exchange for clean ownership.

Expect **very little** ad revenue at v1's scale. AdMob does not pay out below
$100. The job of v1 is users, reviews, and finding out whether tradesmen reach
for this on a real job.

---

## 🔴 The one thing that had to ship in v1

**`Settings.firstRunAt`** — the date this user first ran the app.

When the paywall returns, everyone already using the app must keep what they
have. Silently taking features back from existing users is the surest way to turn
a working app into one-star reviews, and it would land at exactly the moment
there are finally users worth keeping.

Answering *"was this person here before the cutoff?"* is **impossible unless the
app wrote it down at the time**. It cannot be added alongside the paywall that
needs it. That is why a value nothing currently reads ships in v1.

It never leaves the device. It is not analytics.

---

## Turning the paywall back on

**Prerequisite: the app has been transferred to the owner's own Play Console
account.** Transfer first, monetise second — that is the entire point of the
exercise. Play's app transfer preserves installs, ratings, reviews and the Play
App Signing key.

Then:

1. Set `Monetization.PAYWALL_ENABLED = true`.
2. **Add the grandfather rule.** Not optional, and not implemented yet — v1 ships
   the hook, not the rule. Users whose `firstRunAt` precedes the cutoff are
   entitled permanently, without paying and without a Play lookup (there is no
   server, and they never bought anything for Play to confirm).
3. Create the product in Play Console: `stockcut_unlock`, one-time,
   non-consumable. The ID is already in `BillingManager.UNLOCK_PRODUCT_ID` and is
   permanent once created.
4. Add license testers and **walk the full billing matrix** — purchase,
   acknowledge, restore, refund, pending, offline. None of it has ever been run
   against real Play; it is written and unit-tested only.
5. Update the Play data safety form and the store listing: the app now contains
   in-app purchases.
6. Re-enable the Settings "Purchase" section and "Restore purchases" — both are
   already written, behind the same flag.

### What is deliberately still in the codebase

`BillingManager`, `PaywallSheet`, `PaywallHost`, every `PaywallTrigger`, the
tier gates, and the Settings purchase section are **all still present and still
compiled**, behind the flag.

They were not deleted because unpicking billing across four screens and rewriting
it in six months is far more likely to go wrong than flipping one constant. The
tests reflect this: [`PaywallRulesTest`](../data/src/test/kotlin/com/stockcut/data/EntitlementTest.kt)
exercises every dormant rule with `paywallEnabled = true` on every build, so the
paywall does not come back after six months of rotting untested.

`EntitlementTest` asserts `PAYWALL_ENABLED == false` outright — so switching it on
fails the build loudly, with a message pointing here. That is intentional. It
should be a deliberate act, never a quiet one.

### The BILLING permission is still in the manifest

`com.android.vending.BILLING` is merged in by the Play Billing library and has
been left there. It is unused in v1 and declaring it is not a policy problem —
Play derives the store listing's "In-app purchases" badge from the products that
actually exist in the console, not from this permission, and there are none.

It was left rather than stripped with `tools:node="remove"` for the same reason
as everything else here: removing it creates a step that has to be remembered and
reversed later, in exchange for tidying a line nobody reads.

**Declare "In-app purchases: No" on the listing regardless.** That answer is
about products, and it is correct.
