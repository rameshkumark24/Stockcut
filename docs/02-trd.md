# TRD — StockCut

**Date:** 2026-08-04 · **Upstream:** [`01-prd.md`](01-prd.md)

---

## 1. Architecture

Three modules. The optimizer knows nothing about Android — that is the single most important architectural rule in this document.

```
:app          Compose UI, ViewModels, navigation, billing, ads, PDF/share
   │  depends on
:data         Room database, DataStore settings, entitlement cache, repositories
   │  depends on
:optimizer    Pure Kotlin/JVM. Zero Android dependencies. The product.
:units        Pure Kotlin/JVM. Parsing, formatting, conversion.
```

**Why the split:** `:optimizer` and `:units` run in a plain JVM test task — no emulator, no instrumentation, tests execute in milliseconds. You will run them hundreds of times. If they depend on Android, you will not.

**Rule:** if `:optimizer` or `:units` ever needs `import android.*`, the design is wrong. Fix the design.

## 2. Stack and why

| Layer | Choice | Why |
|---|---|---|
| Language | **Kotlin** | Native Android, first-class Billing/AdMob, Claude writes it well |
| UI | **Jetpack Compose** + Material 3 | Fastest path for a small app; dynamic colour and font scaling handled for free |
| Architecture | MVVM — `ViewModel` + `StateFlow`, unidirectional data flow | Standard, testable, no framework to learn |
| DI | **Manual** (a single `AppContainer`) | Hilt is overhead for four screens. Do not add it. |
| Persistence | **Room** (SQLite) | Projects, stock, parts. Migrations required and tested. |
| Settings | **DataStore (Preferences)** | Global defaults, entitlement cache |
| Navigation | **Navigation-Compose**, type-safe routes | |
| Billing | **Play Billing Library 7.0.0+** | Required for new submissions |
| Ads | **AdMob** + **UMP SDK** | UMP mandatory for EU/UK traffic |
| Crash reporting | **Firebase Crashlytics** | Free; must be declared in the data safety form |
| PDF | **Android `PdfDocument`** (platform API) | No third-party dependency, no licence question |
| Build | Gradle KTS, version catalog | |
| CI | GitHub Actions — build AAB + run `:optimizer`/`:units` tests on every push | |
| Analytics SDK | **None in v1** | Play Console gives installs, ratings and Vitals free. An analytics SDK adds a privacy declaration for data you won't act on. |

**Rejected, with reasons (so this is not re-litigated):**
- *Flutter / React Native* — cross-platform buys nothing for an Android-only app; RN/Expo adds APK size, IAP friction, and EAS cost
- *Hilt / Koin* — over-engineering at this size
- *Retrofit / OkHttp* — there is no network layer. If either appears in `build.gradle`, something has gone wrong
- *A backend of any kind* — this is the entire cost strategy

## 3. The measurement unit decision (critical)

**Internal representation: `Long`, in units of 1/320 mm.** Never floats. Never doubles.

**Why 1/320 mm:** it is the largest unit in which *both* metric and imperial fractions are exact integers.

| Real value | Internal (U) | Exact? |
|---|---|---|
| 1 mm | 320 | ✅ |
| 0.1 mm | 32 | ✅ |
| 1 inch | 8128 | ✅ |
| 1/2" | 4064 | ✅ |
| 1/16" | 508 | ✅ |
| 1/32" | 254 | ✅ |
| **1/64"** | **127** | ✅ |

Derivation: 1" = 25.4 mm = 127/5 mm, so 1/64" = 127/320 mm. The greatest common unit of 1 mm and 127/320 mm is 1/320 mm.

**Consequences:**
- No floating-point drift, ever. `3/4" + 1/4" == 1"` exactly.
- Resolution is 0.003125 mm — far finer than any saw.
- A 12 m bar = 3,840,000 U. A 100 m roll = 32,000,000 U. `Long` has enormous headroom; `Int` would also fit but `Long` costs nothing and removes overflow risk from summation.
- **Parse to `Long` at the input boundary. Format from `Long` at the display boundary. Nothing in between ever sees a unit string or a decimal.**

## 4. Non-functional requirements

| # | Requirement | Target | How verified |
|---|---|---|---|
| NFR-1 | Optimize latency | < 2 s for 200 parts on a low-end device (2–3 GB RAM, Android 8–10) | Benchmark test in `:optimizer` + manual on the real device |
| NFR-2 | Cold start | < 1.5 s to interactive on low-end device | Manual, Android Vitals |
| NFR-3 | APK/AAB size | < 12 MB download | Play Console app size report |
| NFR-4 | Offline | 100% of core function works in airplane mode | Manual E2E in airplane mode |
| NFR-5 | Crash-free rate | ≥ 99.5% | Crashlytics + Android Vitals |
| NFR-6 | ANR rate | Below Play bad-behaviour threshold | Android Vitals |
| NFR-7 | Accessibility | Usable at max system font scale; all tap targets ≥ 48 dp; contrast ≥ 4.5:1 | Manual with font scale at max |
| NFR-8 | Correctness | Length invariant holds for every produced plan, always | Property test (`:optimizer`) |
| NFR-9 | Data durability | Zero project loss across app update and device restore | Migration tests + manual reinstall cycle |
| NFR-10 | Battery / wakeups | No background work at all | No `WorkManager`, no services, no alarms |

## 5. Permissions

| Permission | Needed? | Why |
|---|---|---|
| `INTERNET` | **Yes** | AdMob only. Nothing else. |
| `ACCESS_NETWORK_STATE` | Yes | AdMob requirement |
| `com.android.vending.BILLING` | Yes | Play Billing |
| Storage / camera / location / contacts | **No** | Sharing uses `FileProvider` into the app's cache dir — no storage permission needed |

If any other permission appears in the manifest, treat it as a bug.

## 6. Entitlement matrix

> ⚠️ **Superseded for v1 by
> [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md).**
>
> **StockCut ships completely free — every row in the Free column below is
> lifted, and the app earns only from AdMob.** A new app from an unknown
> developer converts close to nothing on a paid unlock; ads earn less per user
> but earn from every user, and a free tool spreads on site, which is this app's
> only real distribution channel.
>
> The matrix below is not dead: it is exactly what returns when
> `Monetization.PAYWALL_ENABLED` is switched back on, and `PaywallRulesTest`
> pins it on every build. Read it as the plan, not as current behaviour. One
> figure has already changed — the interstitial is every **5th** optimize, not
> every 3rd (see the note on `Limits.INTERSTITIAL_EVERY`).

Replaces the checklist's "Auth & roles matrix" — there are no accounts, only two tiers.

| Capability | Free | Paid ($4.99 one-time) |
|---|---|---|
| Parts per project | 20 | 1000 (performance cap) |
| Saved projects | 1 | Unlimited |
| Stock entries per project | 5 | Unlimited |
| Optimize | ✅ | ✅ |
| Share as image | ✅ | ✅ |
| Export PDF | ❌ | ✅ |
| Ads | Banner + interstitial after every 3rd optimize | None |
| All units incl. fractional inch | ✅ | ✅ |
| Kerf + trim | ✅ | ✅ |

**Rules:**
- The free tier must be **genuinely useful**, not crippled. A 20-part job is a real job. The paywall sells scale, not basic function.
- **Never gate correctness.** The optimizer is identical in both tiers.
- Entitlement is cached in DataStore and is authoritative offline. Re-verify with Play on app start when a network is available; **never downgrade a user because a check failed offline.**
- Purchase acknowledgement within 3 days is mandatory or Google auto-refunds — acknowledge immediately on purchase.

## 7. Environment & config plan

Replaces the checklist's dev/staging/prod. There is no server, so "environment" means **build variant**.

| | `debug` | `release` (closed test) | `release` (production) |
|---|---|---|---|
| AdMob unit IDs | **Google test IDs** | **Google test IDs** | Real IDs |
| Billing | License testers | License testers | Live |
| Crashlytics | Off | On | On |
| Logging | Verbose | Off | Off |
| `applicationIdSuffix` | `.debug` | — | — |
| Signing | debug keystore | upload keystore | upload keystore |

**Config that must never be in git:**
- `keystore.properties` (keystore path + passwords)
- `upload-keystore.jks`
- `google-services.json` — *technically shippable, but keep it out of a public repo*
- Real AdMob unit IDs → keep in `local.properties`, injected via `buildConfigField`

Add all of these to `.gitignore` **before the first commit**, per checklist Phase 4. Commit a `keystore.properties.example` with keys and no values.

> ⚠️ **Clicking your own live ads terminates your AdMob account and forfeits earnings.** Test IDs in every non-production variant is a hard rule, not a convention.

## 8. Keystore & signing

1. Enrol in **Play App Signing** at first upload — Google holds the app signing key
2. Generate `upload-keystore.jks` locally; this is the only key you hold
3. Back it up + its passwords in **two places off this machine** (password manager + encrypted cloud)
4. **Losing it means the app can never be updated again.** Not difficult to recover — impossible.
5. `versionCode` monotonically increasing, never reused; `versionName` semver

## 9. Third-party dependency policy

Per checklist Phase 4: every package is verified to exist, be maintained, and have real download numbers before it enters `build.gradle.kts`. AI hallucinates package names and attackers pre-register them.

**Allowed dependency surface for v1** — anything outside this list needs a written reason:
`androidx.core` · `androidx.lifecycle` · `androidx.activity-compose` · `androidx.compose.*` (BOM) · `androidx.navigation:navigation-compose` · `androidx.room` · `androidx.datastore:datastore-preferences` · `com.android.billingclient:billing-ktx` · `com.google.android.gms:play-services-ads` · `com.google.android.ump:user-messaging-platform` · `com.google.android.play:review` · `com.google.firebase:firebase-crashlytics` · `junit` / `kotlin-test` / `kotest-property` (test only).

Lock file committed. No blind auto-upgrades.

## 10. Error handling

One shape for all user-facing failures. The optimizer never throws for expected conditions — it returns them.

| Condition | Behaviour |
|---|---|
| Part longer than any stock (after trim) | `OptimizeResult.Infeasible` listing the offending parts. **Never silently dropped.** |
| Limited stock insufficient | `OptimizeResult.Shortfall` — plan produced for what fits, plus a list of unplaced parts and how much more stock is needed |
| Invalid input (zero, negative, unparseable) | Blocked at the input field with an inline message; never reaches the optimizer |
| Kerf ≥ shortest part | Warning before optimize; allowed but flagged |
| Room migration failure | Never destructive. `fallbackToDestructiveMigration` is **banned** — write the migration. |
| Billing unavailable | App fully usable; unlock button shows a retry message |
| Ad fails to load | Layout collapses gracefully; no blank reserved space, no crash |

## 11. What this app deliberately does not have

No server · no accounts · no user-generated content · no moderation · no background work · no push notifications · no deep links · **no WebView** · no dynamic feature modules · no A/B testing framework.

Each absence is a cost, a compliance burden, or a failure mode that does not exist.

## 12. Data collection

**No personal data is collected by this app.** That statement is load-bearing — it is why §11 is as short as it is, and it is a deliberate constraint, not a happy accident.

| Source | Data | Personal? |
|---|---|---|
| Feedback form ([`09`](09-feedback-channel.md)) | App/OS/device version, unit mode, tier, optimize count, free text | ❌ — no contact field of any kind |
| AdMob | Advertising ID | Per UMP consent |
| Crashlytics | Crash logs, device state | ❌ |
| `mailto:` support link | Whatever the user chooses to write, from their own mail app | User-supplied, outside the app |

**There is no feedback form.** The only contact channel is the `mailto:` link, which means the user hands over their address themselves, through their own mail client, entirely outside this app.

**Architectural note:** the form opens via `Intent.ACTION_VIEW` in the user's browser, so *the app process never transmits anything* — the browser does, after the user has read the diagnostics string on screen and had the chance to delete it. That is a stronger position than an in-app POST, and it is why WebView is banned here rather than merely discouraged.

You still receive the responses, so the form is declared on the Play data safety form — but with no contact field it adds no category that Crashlytics doesn't already require. See [`09-feedback-channel.md`](09-feedback-channel.md) §6.

**Never added to the diagnostics string:** advertising ID · install ID · location · project contents · anything the user cannot see before sending.

## 13. Threat model

Small on purpose. Listed so effort goes where the risk actually is.

| Threat | Real? | Response |
|---|---|---|
| Feedback-form spam | Yes, mild | Soft cooldown + redirect kill switch — [`09`](09-feedback-channel.md) §9 |
| APK decompiled, form URL and AdMob unit IDs extracted | Yes | Neither is secret. Assume both are public. |
| **Paid unlock cracked** | **Yes — unavoidable** | See §13.1 |
| Modified APK republished elsewhere | Yes | Out of your control. Play protects the listing, not the binary. |
| User data breach | **No** | No server, no accounts, no personal data. Nothing to breach. |
| Someone else's data exposed to a user | **No** | No shared storage. Each device is an island. |
| DoS / traffic overload | **No** | No shared resource. See §13.2 |
| SQL injection, XSS, CSRF, IDOR | **No** | No API, no WebView, no multi-user data, parameterised Room queries only |

### 13.1 The paid unlock will be cracked, and that is fine

Entitlement is cached locally so paid users work offline (§6). A rooted device or a patched APK can flip that flag. **This cannot be prevented without a server, and there is no server.**

**Posture: accept it.**

- Someone who patches an APK to avoid a $4.99 one-time purchase was never going to buy it. Cracking does not convert to lost revenue.
- Reasonable and cheap: **R8 obfuscation on release builds**, and re-verify entitlement with Play Billing on launch when a network is available.
- 🔴 **Never punish a suspected pirate at the risk of a false positive.** A genuine paying customer wrongly locked out writes a 1-star review that costs you far more than the pirate ever would. Rule 10 in `CLAUDE.md` — never downgrade on a failed check — exists for exactly this reason.
- Do not add tamper detection, root detection, or licence-check dialogs. They generate false positives, break on custom ROMs, and annoy the people who paid you.

### 13.2 There is no "peak traffic"

Every user runs an independent copy on their own device. There is no shared server, database, connection pool, or queue. Ten users and ten million users are the identical situation — nobody waits for anybody, nothing slows down, nothing can go down.

**Performance in this app is a single-device question**, not a concurrency question: can the optimizer handle 1000 parts on a low-end phone within budget (NFR-1)? That is the whole of it. There is nothing to load test.

### 13.3 What *does* scale badly: a bad release

The one thing that reaches every user at once is **a broken build** — and with no server there is no remote kill switch or feature flag to disable it. Store review means a fix takes days, not minutes.

**This is the real cost of the ₹0 architecture, and the mitigations are already in the plan:**
- **Staged rollout at 20%**, never straight to 100%
- Android Vitals watched hourly for the first 48 h
- Halt the rollout in Play Console if crash rate moves — 80% of users never receive the bad build
- The `:optimizer` property tests, which exist precisely because a silently wrong cut plan is the failure that a staged rollout would *not* catch
