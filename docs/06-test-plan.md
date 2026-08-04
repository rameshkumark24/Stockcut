# Test Plan — StockCut

**Premise:** the optimizer *is* the product. A wrong cut plan costs a tradesman real material and earns a 1-star review that never expires. Testing effort is allocated accordingly: **the majority goes to `:optimizer` and `:units`, which are pure JVM and run in milliseconds.**

---

## 1. Test pyramid for this app

| Layer | Tool | Count | Runs in |
|---|---|---|---|
| **`:optimizer` unit + property** | JUnit5 + kotest-property | ~60 | < 2 s, JVM, no emulator |
| **`:units` unit + property** | JUnit5 + kotest-property | ~40 | < 1 s, JVM |
| `:data` migration tests | Room testing artifact | ~6 | instrumented |
| ViewModel tests | JUnit + Turbine | ~15 | JVM |
| Compose UI tests | Compose test rule | ~8 (critical path only) | instrumented |
| Manual device matrix | Human | see §6 | — |

**Do not write Compose UI tests for everything.** They are slow and brittle. Cover the critical path and rely on the manual matrix for the rest.

---

## 2. `:optimizer` — the tests that matter

### 2.1 The invariant property test *(the single most important test in the project)*

```kotlin
// For ANY valid input, for EVERY bar in the resulting plan:
//   Σparts + (cutCount × kerf) + offcut + trim == stockLength
checkAll(validOptimizeRequests()) { req ->
    val result = optimize(req)
    val plan = result.planOrNull() ?: return@checkAll
    plan.bars.forEach { bar ->
        val sum = bar.parts.sumOf { it.lengthU } +
                  bar.cutCount * req.kerfU +
                  bar.offcutU +
                  bar.trimU
        sum shouldBe bar.stockLengthU
    }
}
```

Run with **≥ 1000 generated cases**. This one test catches nearly every real arithmetic bug — off-by-one kerf, double-counted trim, lost remainder.

### 2.2 Conservation property

Every requested part appears **exactly once** across `plan.bars` + `unplacedParts`. Nothing duplicated, nothing lost.

> This guards the worst possible bug: a plan that silently omits a part. The user cuts everything, then discovers two pieces were never in the plan.

### 2.3 Non-negativity property

`offcutU >= 0` for every bar. A negative offcut means an over-packed bar — a plan that cannot physically be cut.

### 2.4 Determinism property

`optimize(req) == optimize(req)` for the same input, across 100 runs. Catches accidental `Random`, set-iteration-order, or time-based tie-breaking.

### 2.5 Known-answer oracle set

A committed table of hand-verified cases. **Build this with your real tradesman contact** — their actual jobs are the best oracle you will ever get.

| Case | Input | Expected |
|---|---|---|
| `O-01` | Stock 6000, parts 4×1500, kerf 0 | 1 bar, offcut 0, cutCount 3 |
| `O-02` | Stock 6000, parts 4×1500, kerf 3 | 2 bars (4 pieces + kerf > 6000) |
| `O-03` | Stock 6000, part 1×6000, kerf 3 | 1 bar, offcut 0, **cutCount 0** |
| `O-04` | Stock 2000, part 1×2400 | `Infeasible` |
| `O-05` | Stock 6000 ×2 (limited), parts 10×1000, kerf 3 | `Shortfall`, needs 1 more |
| `O-06` | Mixed stock 6000 + 3000 unlimited, parts 2×2900 | Uses 3000s, not 6000s |
| `O-07` | Trim 50, stock 6000, part 1×5960 | `Infeasible` (usable 5950) |
| `O-08` | Kerf 0, 100 identical parts | Exact fit, no waste |
| `O-09` | 8' stock, parts in fractional inches | Round-trip exact through the units module |
| `O-10` | Real job from the tradesman contact | Their hand answer |

### 2.6 Quality regression test

Waste % for the oracle set is recorded in a committed baseline file. **A change that makes any case worse fails the build.** This stops "improvements" that quietly regress packing quality.

### 2.7 Benchmark tests

| Parts | Budget |
|---|---|
| 20 | < 50 ms |
| 200 | < 500 ms on CI (proxy for < 2 s on a low-end device) |
| 1000 | < 10 s |

---

## 3. `:units` — round-trip is everything

### 3.1 Round-trip property

```kotlin
checkAll(Arb.long(1..3_840_000), Arb.of(UnitSystem.entries), Arb.of(8, 16, 32, 64)) { u, sys, denom ->
    val snapped = snapToDenominator(u, sys, denom)
    parse(format(snapped, sys, denom), sys) shouldBe ParseResult.Ok(snapped)
}
```

### 3.2 Exactness cases

| Input | Expected U |
|---|---|
| `1` mm | 320 |
| `1"` | 8128 |
| `1/2"` | 4064 |
| `1/16"` | 508 |
| `1/64"` | 127 |
| `3/4" + 1/4"` | == `1"` exactly |
| `8' 3 1/2"` | 8×12×8128 + 3×8128 + 4064 |

### 3.3 Fraction reduction

`8/16` → `1/2` · `16/16` → whole number, no fraction shown · `0/16` → omitted entirely

### 3.4 Parse rejection

Empty · `-5` · `0` · `abc` · `1/0` · `1//2` · `1 5/` · `"` alone → all `ParseResult.Error` with a usable message.

---

## 4. Data layer

- **Migration test for every schema version bump** — populate vN, migrate, assert data intact. Room's `MigrationTestHelper`.
- Cascade delete: deleting a project removes its stock and parts, leaves other projects untouched.
- CHECK constraints actually reject bad rows (test at the DAO level, not just in Kotlin).
- `exportSchema` JSON committed and diffed in review.

---

## 5. Billing and ads — test before submission, not after

| Case | Expected |
|---|---|
| Purchase with a license tester account | Unlocks immediately, snackbar, ads gone |
| Purchase, then **force-stop and reopen offline** | Still unlocked (DataStore cache) |
| Purchase, uninstall, reinstall, **Restore purchases** | Unlocks |
| Purchase cancelled mid-flow | No entitlement change, no nag |
| Refund issued in Play Console | Entitlement revoked on next online launch |
| Billing service unavailable | App fully usable free; retry message shown |
| Acknowledgement | Purchase acknowledged **immediately** (unacknowledged > 3 days = auto-refund by Google) |
| UMP consent — EU/UK IP | Consent form shows before any ad request |
| UMP consent — non-EU | No form, ads load |
| Ad fails to load | Container collapses, no blank box, no crash |
| **Test ad units in every non-production build** | Verified in `debug` and closed-test builds |

> ⚠️ Never click a live ad in your own app. AdMob termination is permanent and forfeits earnings.

---

## 6. Manual device matrix

| Dimension | Must test |
|---|---|
| **Low-end real device** | 2–3 GB RAM, Android 8–10 — *this is the actual user's phone* |
| Modern device | Android 15/16 |
| Screen | Small (5") and large (6.7") |
| **Font scale at maximum** | Every screen scrolls, nothing clipped |
| Dark mode | Cut-plan segments stay distinguishable |
| Rotation | State preserved |
| **Airplane mode** | Full core function |
| Process death | Background 20 min, return to the same state |
| Sunlight | Read the cut plan outdoors — the contrast test that matters |

---

## 7. Critical-path E2E (Compose UI tests)

Only these. Everything else is manual.

1. First run → tap example job → see a cut plan
2. New job → add 3 parts → add 1 stock → optimize → plan appears
3. Free tier → add 21st part → paywall appears
4. Enter `1 5/16` in fractional mode → displays `1 5/16"` after blur
5. Part longer than stock → infeasible banner, **no navigation to the plan**
6. Delete part → undo snackbar → part restored
7. Optimize → share image → chooser opens
8. Rotate on the result screen → plan unchanged, not recomputed

---

## 8. Edge-case checklist (from the checklist's Phase 7)

- Empty inputs · maximum lengths (100 m) · 1000 parts · unicode and emoji in labels · SQL characters in labels (`'; DROP TABLE`) · concurrent double-tap on Optimize · double-tap on the purchase button · very long job names · 40+ bar plans (identical-bar collapsing)

---

## 9. Definition of Done

A feature is done when **all** of these are true:

- [ ] Unit tests pass, including any new property tests
- [ ] The four states (loading/empty/error/success) are implemented, not just the happy path
- [ ] Works at max font scale
- [ ] Works in dark mode
- [ ] Works in airplane mode
- [ ] No new permission added
- [ ] No `Float`/`Double` outside `wastePercent` and drawing coordinates
- [ ] No new dependency outside the allowed list in the TRD
- [ ] Manually run once on the low-end device
- [ ] Committed, with the AI-generated code read line by line — per checklist Phase 4, never merge code you can't explain

## 10. Release gate — all must be green before production submission

- [ ] Full `:optimizer` and `:units` suites green, ≥ 1000 property cases
- [ ] Waste-% baseline not regressed
- [ ] Migration tests green
- [ ] Billing matrix (§5) fully walked with a license tester
- [ ] UMP consent verified for an EU IP
- [ ] Test ad IDs confirmed absent from the production variant
- [ ] Device matrix (§6) walked
- [ ] `targetSdk = 36` confirmed in the built AAB
- [ ] Keystore backed up in two off-machine locations
- [ ] Uninstall/reinstall cycle: projects restored via auto-backup
- [ ] Crash-free rate ≥ 99.5% across the 14-day closed test
- [ ] Data safety form matches reality (AdMob + Crashlytics declared)
- [ ] Privacy policy live at a public URL
- [ ] **≥ 3 of the 12 testers are real tradesmen, not just friends**
