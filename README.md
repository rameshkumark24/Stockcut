# StockCut

**1D linear cut-list optimizer for Android.** Offline. No backend.

Give it the stock lengths you have and the pieces you need — it works out which pieces come off which bar with the least waste, accounting for saw kerf.

Built for fabricators, welders, timber framers, and anyone cutting from linear stock: tube, angle, rebar, extrusion, studs, trim.

---

## Status

🟡 **Phases 3–5 complete — the engine, the data layer and the whole UI are built and tested.**

| Module | State |
|---|---|
| `:units` | Done — 5 unit systems, fractional inch, exact integer arithmetic |
| `:optimizer` | Done — Best-Fit-Decreasing + improvement pass, self-verifying invariant |
| `:data` | Done — Room schema, DB-level constraints, DataStore, entitlement rules |
| `:app` | Done — 5 routes, all 7 screens, share as image, PDF export |

167 tests green: 126 on the JVM, 41 instrumented.

**Next: Phase 6 — billing and ads.** Almost none of it can be *tested* until a
Play Console account, an in-app product and license testers exist. See
[`docs/10-owner-actions.md`](docs/10-owner-actions.md) for the step-by-step.

🔴 **Two W0 gates are still open and both are non-code** (see [`docs/07-implementation-plan.md`](docs/07-implementation-plan.md) W0). Phases 3–4 were built ahead of them:

- [ ] A real tradesman validates a hand-made cut plan on paper — *if this fails, the project stops*
- [ ] App name cleared against the Play Store, package name fixed — **permanent once published**

Also open, and the longest lead-time item: **tester recruitment**. A closed test needs 12 testers opted in for 14 continuous days before production access can even be applied for.

---

## Why it exists

A tradesman buys 6 m steel tube or 16 ft studs and works out the cutting order in his head. The result is 10–25% waste. On a job with £2,000 of steel that is £200–500 in the offcut bin — every job.

The tools that solve this are on iOS, in a browser (needs signal and a keyboard), or built for cabinetmakers cutting plywood. Not for someone standing next to a chop saw with no signal.

## What makes it different

| | |
|---|---|
| **1D linear, not 2D panels** | Metal and framing trades, not cabinetmakers |
| **Offline** | Works in a workshop with no signal |
| **Fractional inches** | Type `1 5/16"` — no converting to decimals |
| **Exact arithmetic** | Integer maths at 1/320 mm. `3/4" + 1/4" == 1"`, exactly, always |

## Architecture

```
:app         Compose UI, ViewModels, navigation, billing, ads, PDF/share
:data        Room, DataStore, repositories, entitlement cache
:optimizer   Pure Kotlin/JVM. Zero Android dependencies. The product.
:units       Pure Kotlin/JVM. Parsing, formatting, conversion.
```

`:optimizer` and `:units` never import `android.*` — their tests run in milliseconds on the JVM, with no emulator.

**No server. No accounts. No user data collected.** The only network call is the ads SDK.

## Documentation

| Doc | What's in it |
|---|---|
| [`docs/00-phase-0-scope-feasibility.md`](docs/00-phase-0-scope-feasibility.md) | Problem, market, competitors, cost model, risks, go/no-go |
| [`docs/00-gap-audit.md`](docs/00-gap-audit.md) | Play Store release gates, keystore, billing, ads policy, ASO |
| [`docs/01-prd.md`](docs/01-prd.md) | Personas, user stories, v1 scope, out-of-scope list |
| [`docs/02-trd.md`](docs/02-trd.md) | Stack, NFRs, entitlement matrix, build variants |
| [`docs/03-app-flow.md`](docs/03-app-flow.md) | Every screen, route, and empty/error state |
| [`docs/04-uiux-brief.md`](docs/04-uiux-brief.md) | Design tokens, components, microcopy, store assets |
| [`docs/05-data-model-and-optimizer-contract.md`](docs/05-data-model-and-optimizer-contract.md) | Schema + the optimizer/units module contracts |
| [`docs/06-test-plan.md`](docs/06-test-plan.md) | Property tests, oracle set, device matrix, release gate |
| [`docs/07-implementation-plan.md`](docs/07-implementation-plan.md) | 9-week plan, milestones, gates |
| [`docs/08-build-checklist.md`](docs/08-build-checklist.md) | **The executable checklist. Work from this one.** |
| [`docs/09-feedback-channel.md`](docs/09-feedback-channel.md) | In-app feedback via Google Form, and its compliance impact |
| [`docs/10-owner-actions.md`](docs/10-owner-actions.md) | **Everything that must happen outside the code, in dependency order** |
| [`docs/11-play-and-admob-setup.md`](docs/11-play-and-admob-setup.md) | Step-by-step: in-app product, license testers, AdMob, Firebase, keystore |
| [`site/`](site/) | The privacy policy and feedback redirect, ready to publish on GitHub Pages |
| [`CLAUDE.md`](CLAUDE.md) | Agent rules — hard constraints, conventions, do-nots |

## Build

```bash
# Correctness suite — pure JVM, no emulator, seconds
./gradlew :units:test :optimizer:test :data:testDebugUnitTest

# Cascade deletes, CHECK constraints, migrations — needs a device or emulator.
# CI only COMPILES these, so run them yourself before any schema change.
./gradlew :data:connectedDebugAndroidTest
```

## Setup

Copy `keystore.properties.example` → `keystore.properties` and fill it in. Never commit the real file.

---

## Licence

Not yet decided. All rights reserved for now.
