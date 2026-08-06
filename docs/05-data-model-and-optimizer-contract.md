# Data Model + Optimizer Contract — StockCut

Covers two checklist items at once: **Backend schema** (here: local schema) and **API contract** (here: the module contract, since there is no API).

---

# Part 1 — Local data model

All data is on-device. Room / SQLite. No server, no sync, no user table.

## 1.1 ERD

```
Project ─1──────n─ StockEntry
   │
   └─1──────n─ PartEntry

StockProfile   (standalone — saved reusable stock sizes)
```

Four tables. That is the entire schema — three for a job, plus the standalone
`stock_profile` list of reusable stock sizes (US-14).

## 1.2 Tables

### `project`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | INTEGER | PK, autoincrement | |
| `name` | TEXT | NOT NULL | |
| `unit_system` | TEXT | NOT NULL | `MM`·`CM`·`M`·`INCH_DECIMAL`·`INCH_FRACTIONAL` |
| `fraction_denominator` | INTEGER | NOT NULL, default 16 | 8·16·32·64 |
| `kerf_u` | INTEGER | NOT NULL, default 960 | **Internal units** (960 U = 3 mm) |
| `trim_u` | INTEGER | NOT NULL, default 0 | Removed from the start of each stock length |
| `is_example` | INTEGER | NOT NULL, default 0 | The seeded first-run example job |
| `created_at` | INTEGER | NOT NULL | epoch millis, **UTC** |
| `updated_at` | INTEGER | NOT NULL | epoch millis, **UTC** |

### `stock_entry`

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK |
| `project_id` | INTEGER | NOT NULL, FK → `project.id` **ON DELETE CASCADE**, indexed |
| `length_u` | INTEGER | NOT NULL, CHECK > 0 |
| `quantity` | INTEGER | NOT NULL — `-1` means **unlimited** |
| `label` | TEXT | nullable |
| `sort_order` | INTEGER | NOT NULL |

### `part_entry`

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK |
| `project_id` | INTEGER | NOT NULL, FK → `project.id` **ON DELETE CASCADE**, indexed |
| `length_u` | INTEGER | NOT NULL, CHECK > 0 |
| `quantity` | INTEGER | NOT NULL, CHECK > 0 |
| `label` | TEXT | nullable |
| `sort_order` | INTEGER | NOT NULL |

### `stock_profile`

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK |
| `name` | TEXT | NOT NULL — "50×50 SHS 6 m" |
| `length_u` | INTEGER | NOT NULL, CHECK > 0 |
| `last_used_at` | INTEGER | NOT NULL |

## 1.3 Rules

- **Every length is `INTEGER`, in internal units of 1/320 mm.** No REAL columns anywhere in this schema. Floating point does not appear in the persistence layer.
- **Constraints live in the database**, not only in Kotlin — `NOT NULL`, `CHECK`, `ON DELETE CASCADE`, per checklist Phase 3.
- **How `CHECK` is implemented:** Room's annotations cannot emit a `CHECK` clause and SQLite has no `ALTER TABLE ... ADD CONSTRAINT`, so each `CHECK` above is a `BEFORE INSERT` / `BEFORE UPDATE` trigger calling `RAISE(ABORT)`, in `SchemaConstraints.kt`. A rejected row raises `SQLiteConstraintException`, exactly as a `CHECK` would. Triggers are invisible to Room's schema validation (which reads `PRAGMA table_info`, `foreign_key_list` and `index_list`), so they do not appear in the exported schema JSON and cannot cause an identity-hash mismatch. 🔴 **A migration that adds a table must create that table's triggers in the same `Migration`** — `onCreate` does not run again for an existing install.
- Timestamps stored as epoch millis **UTC**; converted at display time only.
- **Hard delete** for parts and stock (an undo snackbar holds the row in memory, not in the DB). **Hard delete** for projects too — there is no recovery expectation and no server.
- Indexes on both `project_id` foreign keys. That is all the indexing this app will ever need.

## 1.4 Migrations

- `fallbackToDestructiveMigration()` is **banned.** It silently deletes a tradesman's saved jobs on upgrade.
- Every schema change ships a written `Migration` object and a migration test.
- Room `exportSchema = true`; the generated JSON schemas are **committed to git** — they are the only record of what shipped to users.
- Android auto-backup enabled, verified by a real uninstall/reinstall cycle before release.

## 1.5 Settings (DataStore, not Room)

| Key | Type | Default |
|---|---|---|
| `default_unit_system` | String | `MM` |
| `default_fraction_denominator` | Int | 16 |
| `default_kerf_u` | Long | 960 (3 mm) |
| `theme` | String | `SYSTEM` |
| `is_unlocked` | Boolean | false |
| `optimize_count` | Int | 0 — drives the ad cadence and review prompt |
| `last_review_prompt_at` | Long | 0 |
| `example_project_deleted` | Boolean | false |

`is_unlocked` is the **offline entitlement cache**. It is authoritative when there is no network. It may be set true by a Play verification; it must **never** be set false because a verification failed offline.

---

# Part 2 — Optimizer module contract

`:optimizer` — pure Kotlin/JVM, zero Android dependencies. This is the product; everything else is packaging.

## 2.1 Public surface

The entire module exposes **one function**.

```kotlin
package com.stockcut.optimizer

/** All lengths are Long, in internal units of 1/320 mm. */
data class StockSpec(
    val id: Long,
    val lengthU: Long,          // > 0
    val quantity: Int,          // > 0, or UNLIMITED
    val label: String? = null,
) { companion object { const val UNLIMITED = -1 } }

data class PartSpec(
    val id: Long,
    val lengthU: Long,          // > 0
    val quantity: Int,          // > 0
    val label: String? = null,
)

data class OptimizeRequest(
    val stock: List<StockSpec>,
    val parts: List<PartSpec>,
    val kerfU: Long,            // >= 0
    val trimU: Long = 0,        // >= 0, removed from the start of each stock length
)

data class PlacedPart(val partId: Long, val lengthU: Long, val label: String?)

data class CutBar(
    val stockId: Long,
    val stockLengthU: Long,
    val trimU: Long,
    val parts: List<PlacedPart>,   // in cut order, first cut first
    val cutCount: Int,             // number of saw cuts, kerf-consuming
    val offcutU: Long,             // >= 0
)

data class Plan(
    val bars: List<CutBar>,
    val totalStockUsedU: Long,
    val totalPartsU: Long,
    val totalKerfU: Long,
    val totalTrimU: Long,
    val totalOffcutU: Long,
    val wastePercent: Double,      // (kerf + trim + offcut) / totalStockUsed * 100
)

sealed interface OptimizeResult {
    /** Every requested part was placed. */
    data class Success(val plan: Plan) : OptimizeResult

    /** One or more parts are longer than any available stock (after trim). Nothing was planned. */
    data class Infeasible(val impossibleParts: List<PartSpec>, val longestUsableU: Long) : OptimizeResult

    /** Limited stock ran out. A partial plan is still returned and is still useful. */
    data class Shortfall(
        val plan: Plan,
        val unplacedParts: List<PartSpec>,
        val additionalStockNeeded: Map<Long, Int>,   // stockId -> extra bars required
    ) : OptimizeResult

    /** Input violated a precondition. Should be unreachable — the UI validates first. */
    data class InvalidInput(val reason: String) : OptimizeResult
}

fun optimize(request: OptimizeRequest): OptimizeResult
```

**This function never throws for an expected condition.** Every failure mode is a return value. A thrown exception from `:optimizer` is a bug, by definition.

## 2.2 Kerf and trim model — the physical truth

For one bar holding parts `p₁…pₙ`:

```
usable       = stockLength − trim
consumed     = Σpᵢ + (cutCount × kerf)
cutCount     = n      if offcut > 0     (a final cut separates the last part from the offcut)
             = n − 1  if offcut == 0    (the last part ends at the bar's end; no final cut)
offcut       = usable − Σpᵢ − (cutCount × kerf)
```

**The invariant that must hold for every bar in every plan, always:**

```
Σparts + (cutCount × kerf) + offcut + trim == stockLength
```

This one equation is the app's correctness guarantee. It is the property test in [`06-test-plan.md`](06-test-plan.md), and it catches nearly every real arithmetic bug.

## 2.3 Algorithm

**v1: Best-Fit-Decreasing + a local improvement pass.** Not optimal — the 1D cutting-stock problem is NP-hard — but consistently within a few percent of optimal and fast enough to feel instant.

```
1. Expand quantities into a flat list of individual parts.
2. Sort descending by length.
3. For each part:
     find the open bar with the SMALLEST remaining space that still fits (part + kerf)
     if none fits → open a new bar of the largest stock size that can hold it
                    (respecting remaining stock quantities)
     if no stock remains → collect into unplacedParts
4. Improvement pass: for each bar with a large offcut, attempt to swap in a
   larger unplaced/smaller-bar part. Cap at 50 iterations — bounded time matters
   more than the last 0.5% of waste.
5. Sort each bar's parts descending (longest cut first — how people actually work).
6. Compute totals, verify the §2.2 invariant, return.
```

**Step 6 is not optional.** The optimizer verifies its own invariant before returning. If it fails, that is an internal error — log it and return `InvalidInput`. Never hand the user a plan that doesn't balance.

**Stock selection when multiple sizes are available:** prefer the size that leaves the least offcut for the current part set. Do not assume one stock size.

**Determinism:** identical input must produce byte-identical output. No `Random`, no set iteration order, no time-based tie-breaking. Ties break on `(length desc, id asc)`.

## 2.4 Performance budget

| Parts | Budget | Where enforced |
|---|---|---|
| 20 (free tier) | < 50 ms | Benchmark test |
| 200 | **< 2 s on a low-end device** (NFR-1) | Benchmark test + manual |
| 1000 (paid cap) | < 10 s, with progress shown | Benchmark test |

Above 1000 parts: reject at the UI layer. Do not attempt.

## 2.5 Edge cases the contract must handle

| Input | Expected result |
|---|---|
| No parts | `InvalidInput` — UI blocks this first |
| No stock | `InvalidInput` — UI blocks this first |
| Part == stock length exactly, trim 0, kerf 0 | `Success`, 1 bar, offcut 0, cutCount 0 |
| Part == stock length, kerf > 0 | Fits — a single part needs no cut if it consumes the whole bar |
| Part = stock − 1 U | Fits, offcut 1 U (kerf applies: cutCount 1) |
| Part longer than longest stock | `Infeasible` |
| Trim ≥ stock length | `InvalidInput` |
| Kerf ≥ shortest part | Valid, but the UI warns before running |
| Kerf 0 | Valid (waterjet, shear) |
| All parts identical | Correct, and fast |
| 1 part, unlimited stock | 1 bar |
| Limited stock, insufficient | `Shortfall` with `additionalStockNeeded` populated |
| Mixed unlimited + limited stock | Limited consumed first, then unlimited |

---

# Part 3 — Units module contract

`:units` — pure Kotlin/JVM.

```kotlin
package com.stockcut.units

/** 1 mm = 320 U. 1 inch = 8128 U. 1/64" = 127 U. All exact. */
const val U_PER_MM = 320L
const val U_PER_INCH = 8128L

enum class UnitSystem { MM, CM, M, INCH_DECIMAL, INCH_FRACTIONAL }

sealed interface ParseResult {
    data class Ok(val valueU: Long) : ParseResult
    data class Error(val message: String) : ParseResult
}

fun parse(input: String, system: UnitSystem): ParseResult
fun format(valueU: Long, system: UnitSystem, denominator: Int = 16): String
```

## 3.1 Accepted input formats

| Unit system | Accepted |
|---|---|
| `MM` / `CM` / `M` | `1200` · `1200.5` · `1,200` (comma stripped) |
| `INCH_DECIMAL` | `47.25` · `47.25"` |
| `INCH_FRACTIONAL` | `3/4` · `1 5/16` · `47` · `8' 3 1/2"` · `8'3.5"` · `3/4"` |

Rejected everywhere: empty, negative, zero, non-numeric, denominator 0.

## 3.2 Formatting rules

- `INCH_FRACTIONAL` rounds to the nearest `1/denominator` and **reduces the fraction** (`8/16` → `1/2`, never `8/16`)
- Whole numbers omit the fraction: `47"`, not `47 0/16"`
- Feet shown when ≥ 12": `8' 3 1/2"`
- `MM` shows no decimals unless the value is not a whole millimetre
- **Round-trip guarantee:** `parse(format(x)) == x` for every value representable in the chosen system and denominator. This is a property test.

## 3.3 Why not floats

`0.1 + 0.2 != 0.3` in IEEE 754. On a 40-part cut list that error accumulates into a plan that doesn't physically cut. `Long` at 1/320 mm makes `3/4" + 1/4" == 1"` exactly true, forever.

**Rule for the whole codebase: `Float` and `Double` may appear only in `wastePercent` and in UI drawing coordinates. Nowhere else.**
