# CLAUDE.md — StockCut

> Agent rules file for this repository, required by Phase 1 of the Vibe Coding checklist.
> Read this before writing any code. Full specs are in [`docs/`](docs/).

---

## What this app is

Android-only, **offline**, 1D linear cut-list optimizer for tradesmen cutting from stock lengths (steel tube, timber studs, rebar, extrusion). Given available stock lengths and required pieces, it produces the cutting plan with the least waste, accounting for saw kerf.

**There is no backend. There never will be.** No server, no accounts, no user data collected by us, no network calls except AdMob.

Full specs live in `docs/` — PRD, TRD, app flow, UI/UX brief, data model + optimizer contract, test plan, implementation plan. **Read the optimizer contract before touching `:optimizer`.**

---

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · Navigation-Compose · Play Billing 7+ · AdMob + UMP · Firebase Crashlytics · Gradle KTS with a version catalog.

`targetSdk = 36` (mandatory for new Play submissions after 2026-08-31) · `minSdk = 26`.

## Module structure

```
:app         Compose UI, ViewModels, navigation, billing, ads, PDF/share
:data        Room, DataStore, repositories, entitlement cache
:optimizer   Pure Kotlin/JVM. ZERO Android dependencies. The product.
:units       Pure Kotlin/JVM. Parsing, formatting, conversion.
```

---

## Hard rules — do not violate these

1. **`:optimizer` and `:units` must never import `android.*`.** They are pure JVM so their tests run in milliseconds without an emulator. If a change seems to require an Android import, the design is wrong — fix the design.

2. **All lengths are `Long`, in internal units of 1/320 mm.** Never `Float`, never `Double`, never a formatted string.
   - `1 mm = 320 U` · `1 inch = 8128 U` · `1/64" = 127 U` — all exact
   - Parse to `Long` at the input boundary; format from `Long` at the display boundary; nothing in between sees a unit
   - `Float`/`Double` are permitted **only** in `wastePercent` and in UI drawing coordinates. Nowhere else.

3. **The kerf invariant must hold for every bar in every plan:**
   ```
   Σparts + (cutCount × kerf) + offcut + trim == stockLength
   ```
   The optimizer verifies this itself before returning. Never return an unbalanced plan.

4. **Never silently drop a part.** A part that cannot be placed is returned in `Infeasible` or `Shortfall`. A plan that quietly omits pieces is the worst possible bug in this app.

5. **`optimize()` never throws for an expected condition.** Every failure is a return value in the `OptimizeResult` sealed interface. A thrown exception from `:optimizer` is a bug.

6. **`fallbackToDestructiveMigration()` is banned.** It deletes a tradesman's saved jobs. Write the migration and its test.

7. **No new permissions.** The manifest has `INTERNET`, `ACCESS_NETWORK_STATE`, `BILLING`. Nothing else. Sharing uses `FileProvider` into the cache dir.

8. **AdMob test ad unit IDs in every non-production build.** Clicking a live ad in your own app terminates the AdMob account permanently and forfeits earnings.

9. **Never gate correctness behind the paywall.** Free and paid run the identical optimizer. The paywall sells scale (more parts, more projects, PDF), never accuracy.

10. **Never downgrade a paid user because an entitlement check failed offline.** The DataStore cache is authoritative when there is no network.

11. **The app collects nothing from its users. There is no feedback form, no bug-report form, no survey, and no in-app submission of any kind.** This is a product decision, not a gap — do not add one back.
    - The only contact channel is a `mailto:` link in About. That is not collection: it opens the user's own mail app, they see and edit the whole message, and nothing arrives unless they choose to send it.
    - The diagnostics line exists solely to be pasted into that email, is **visible and editable on screen**, and must never contain an advertising ID, install ID, location, or project contents.
    - **If you ever add any way for a user to send us data, the Play data safety form and the privacy policy change in the same PR.** An inaccurate declaration gets apps suspended, and it always happens the same way: a field added after the declarations were written.

---

## Conventions

- **Architecture:** MVVM — `ViewModel` + `StateFlow`, unidirectional data flow. Manual DI via a single `AppContainer`. **Do not add Hilt or Koin.**
- **Naming:** length variables carrying internal units end in `U` — `lengthU`, `kerfU`, `offcutU`. This makes a unit mistake visible at the call site.
- **Timestamps:** epoch millis, UTC, converted at display only.
- **Determinism:** identical input produces identical output. No `Random`, no set-iteration order, no time-based tie-breaking in `:optimizer`.
- **Compose:** stateless composables + a state holder. No business logic in composables.
- **Tests:** every `:optimizer`/`:units` change ships with a test. Property tests over example tests where a property exists.

## Do not

- Add a backend, an API client, Retrofit, OkHttp, or any network layer beyond the ads SDK
- Add an analytics SDK in v1
- Add a dependency outside the allowed list in `docs/02-trd.md` §9 without a written reason
- Add 2D panel/sheet cutting — that is v2 and it is a different product
- Add accounts, sync, sharing, or anything requiring a user identity
- Add background work — no `WorkManager`, no services, no alarms
- Use `WebView` — nothing in this app renders remote content, so there is nothing for one to do
- Write "Oops!", exclamation marks, or emoji in product copy — see `docs/04-uiux-brief.md` §1
- Reserve blank space for an ad that failed to load — collapse the container

## Verify before adding any package

AI invents package names and attackers pre-register them (slopsquatting). Before a dependency enters `build.gradle.kts`: confirm it exists, is maintained, and has real download numbers. Lock file committed. No blind auto-upgrades.

---

## Files that must never be committed

`keystore.properties` · `*.jks` · `local.properties` · `google-services.json`

These are in `.gitignore` from the first commit. A committed `.example` variant with keys and no values is fine.

> 🔴 **Losing the upload keystore means the app can never be updated again.** It is backed up in two places off this machine.

---

## Working agreement

- Commit before every AI session — assume the agent may destroy working code
- One branch per feature; small, reviewable commits
- Read every generated migration before running it
- **Never merge code you can't explain line by line**
- Delete dead code and unused files the agent leaves behind

## Definition of done

Tests pass · all four screen states implemented · works at max font scale · works in dark mode · works in airplane mode · no new permission · no `Float`/`Double` outside the two allowed places · no unapproved dependency · manually run once on the low-end device.

---

## Build order

**The optimizer comes first, headless and fully tested, before a single screen exists.** It is the product; the UI is packaging.
