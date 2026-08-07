# Feedback channel — REMOVED

**Status: 🔴 dropped, 2026-08-07. Do not rebuild it.**

This document used to specify an in-app feedback channel: a Google Form opened
through a GitHub Pages redirect, with a pre-filled diagnostics line, a soft
cooldown, and a triage routine. It was built, wired into About, and then removed.

## The decision

**StockCut collects nothing from its users.** No feedback form, no bug-report
form, no survey, no in-app submission of any kind.

## Why

The form was designed to add "no new data-safety category" by having no contact
field. That argument only holds while the form stays exactly that shape — and the
first version created in practice came from a Google template carrying **Name and
Email fields**, which would have made the privacy policy false and the Play data
safety declaration wrong on day one.

That is not a mistake anyone made carelessly. It is the failure mode this
project's own rules predicted: *"it always happens this way — a field added after
the declarations were written."* A channel whose compliance correctness depends
on nobody ever editing a form is a liability that has to be re-verified forever.

Removing it is the stronger position, and it costs less than it appears:

- **Nothing to declare.** With no submissions, the data safety form covers only
  AdMob and Crashlytics — exactly what those SDKs force anyway.
- **Nothing to secure.** No form to flood, no responses to leak, no sharing
  setting to get wrong, no CSV-injection risk when reading exports.
- **No kill switch needed.** The GitHub Pages redirect existed only to be able to
  change or disable the form without an app update. With no form, there is
  nothing to disable.
- **One fewer permission.** `ACCESS_NETWORK_STATE` was added solely for the
  form's offline check and left with it. The app now ships with **zero**
  permissions until Phase 6.

## What replaces it

**The `mailto:` support link in About.** It is not a collection channel: it opens
the user's own mail app, they see and edit the entire message, and nothing
reaches us unless they press send. Play requires a support contact on the store
listing regardless.

The diagnostics line is still shown on the About screen, still visible and
editable, and is added to that email only if the user sends one.

## What was lost, honestly

- **Field 3, "What were you cutting?"** was the most valuable question in the
  form — it was how the metal-trades hypothesis in `docs/00-phase-0` would have
  been tested. That signal now has to come from the closed-test tradesmen and
  from reviews instead.
- **`wrong-plan` reports** were meant to feed oracle case `O-10` in
  `docs/06` §2.5. Correctness reports now arrive by email or not at all, so the
  closed test matters more than it did.

## Consequences recorded elsewhere

- `CLAUDE.md` rule 11 — rewritten as "the app collects nothing"
- `docs/08` Phase 7 — marked not applicable
- `docs/01` US-16, US-17, US-18 — dropped
- `docs/03` S7 and the S4 "This plan looks wrong" entry — removed
- `privacy-policy.html` — the feedback-form section replaced
