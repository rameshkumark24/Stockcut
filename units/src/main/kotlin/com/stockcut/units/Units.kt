package com.stockcut.units

/**
 * Length arithmetic for StockCut.
 *
 * INTERNAL UNIT: 1/320 mm, held in a [Long]. Never a Float, never a Double.
 *
 * 1/320 mm is the largest unit in which BOTH metric and imperial fractions are
 * exact integers. 1 inch = 25.4 mm = 127/5 mm, so 1/64" = 127/320 mm, and the
 * greatest common unit of 1 mm and 127/320 mm is 1/320 mm.
 *
 *   1 mm    = 320 U        1 inch  = 8128 U
 *   0.1 mm  =  32 U        1/2"    = 4064 U
 *                          1/16"   =  508 U
 *                          1/64"   =  127 U
 *
 * Consequence: 3/4" + 1/4" == 1" exactly, forever. Float arithmetic accumulates
 * error across a 40-part cut list and produces plans that do not physically cut.
 */

const val U_PER_MM = 320L
const val U_PER_CM = 3_200L
const val U_PER_M = 320_000L
const val U_PER_INCH = 8_128L
const val U_PER_FOOT = 97_536L // 12 * 8128

/** 100 m. Anything longer is a typo, not a stock length. */
const val MAX_LENGTH_U = 32_000_000L

enum class UnitSystem { MM, CM, M, INCH_DECIMAL, INCH_FRACTIONAL }

/** Denominators offered for fractional-inch display. Each divides 8128 exactly. */
val SUPPORTED_DENOMINATORS = listOf(8, 16, 32, 64)

sealed interface ParseResult {
    data class Ok(val valueU: Long) : ParseResult
    data class Error(val message: String) : ParseResult
}

// ─────────────────────────────────────────────────────────────────────────────
// Display grid
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The smallest displayable step, in internal units. Every grid value is an exact
 * integer number of U, which is what makes [format] -> [parse] lossless.
 */
fun gridU(system: UnitSystem, denominator: Int = 16): Long = when (system) {
    // 0.1 mm — finer than any saw, and 32 U exactly.
    UnitSystem.MM, UnitSystem.CM, UnitSystem.M -> 32L
    // 1/64" = 127 U exactly. Decimal inch still displays as a decimal.
    UnitSystem.INCH_DECIMAL -> 127L
    UnitSystem.INCH_FRACTIONAL -> {
        require(denominator in SUPPORTED_DENOMINATORS) { "Unsupported denominator: $denominator" }
        U_PER_INCH / denominator
    }
}

/** Rounds a value onto the display grid, so it can round-trip exactly. */
fun snap(valueU: Long, system: UnitSystem, denominator: Int = 16): Long {
    val g = gridU(system, denominator)
    return ((valueU + g / 2) / g) * g
}

// ─────────────────────────────────────────────────────────────────────────────
// Parsing
// ─────────────────────────────────────────────────────────────────────────────

private val DECIMAL = Regex("""^(\d+)(?:\.(\d+))?$""")

private const val ERR_METRIC = "Try 1200 or 1200.5"
private const val ERR_INCH_DECIMAL = "Try 47 or 47.25"
private const val ERR_INCH_FRACTIONAL = "Try 1200, 3/4, or 1 5/16"
private const val ERR_POSITIVE = "Length must be more than 0."
private const val ERR_TOO_LONG = "That's longer than 100 m."

/**
 * Parses user input in the given unit system.
 *
 * Accepted:
 *   MM/CM/M         1200, 1200.5, 1,200, "1200 mm"
 *   INCH_DECIMAL    47, 47.25, 47.25"
 *   INCH_FRACTIONAL 47, 3/4, 1 5/16, 8' 3 1/2", 8'3.5", 8'
 *
 * Never throws. Every failure is a [ParseResult.Error] with a message written
 * for the user, not for a log.
 */
fun parse(input: String, system: UnitSystem): ParseResult {
    val s = input.trim().replace(",", "").replace('’', '\'').replace('”', '"')
    if (s.isEmpty()) return ParseResult.Error("Enter a length.")

    val result = when (system) {
        UnitSystem.MM -> parseDecimal(s, U_PER_MM, ERR_METRIC)
        UnitSystem.CM -> parseDecimal(s, U_PER_CM, ERR_METRIC)
        UnitSystem.M -> parseDecimal(s, U_PER_M, ERR_METRIC)
        UnitSystem.INCH_DECIMAL -> parseDecimal(s, U_PER_INCH, ERR_INCH_DECIMAL)
        UnitSystem.INCH_FRACTIONAL -> parseFractional(s)
    }
    return validate(result)
}

private fun validate(r: ParseResult): ParseResult = when {
    r !is ParseResult.Ok -> r
    r.valueU <= 0L -> ParseResult.Error(ERR_POSITIVE)
    r.valueU > MAX_LENGTH_U -> ParseResult.Error(ERR_TOO_LONG)
    else -> r
}

/** Strips any trailing unit suffix ("mm", "cm", "m", double-quote) before parsing. */
private fun parseDecimal(raw: String, perUnit: Long, errorMessage: String): ParseResult {
    val cleaned = raw.trimEnd { !it.isDigit() && it != '.' }.trim()
    val m = DECIMAL.matchEntire(cleaned) ?: return ParseResult.Error(errorMessage)
    return decimalToU(m.groupValues[1], m.groupValues[2], perUnit, errorMessage)
}

private fun decimalToU(
    whole: String,
    frac: String,
    perUnit: Long,
    errorMessage: String,
): ParseResult {
    if (whole.length > 9 || frac.length > 9) return ParseResult.Error(errorMessage)
    val scale = pow10(frac.length)
    val wholeVal = whole.toLongOrNull() ?: return ParseResult.Error(errorMessage)
    val fracVal = if (frac.isEmpty()) 0L else frac.toLongOrNull() ?: return ParseResult.Error(errorMessage)
    // Guard the multiply below; 1e9 * 1e9 would overflow.
    if (wholeVal > MAX_LENGTH_U) return ParseResult.Error(ERR_TOO_LONG)
    val numerator = wholeVal * scale + fracVal
    // Round to nearest U rather than truncating.
    return ParseResult.Ok((numerator * perUnit + scale / 2) / scale)
}

/**
 * Fractional-inch parsing.
 *
 * Grammar, informally:  [feet '] [whole] [num/den] ["]
 * At most one whole part and one fraction, and the whole must precede the fraction.
 */
private fun parseFractional(raw: String): ParseResult {
    var s = raw.replace("\"", " ").trim()
    var totalU = 0L

    val footIdx = s.indexOf('\'')
    if (footIdx >= 0) {
        val feet = s.substring(0, footIdx).trim().toLongOrNull()
            ?: return ParseResult.Error(ERR_INCH_FRACTIONAL)
        if (feet > 400) return ParseResult.Error(ERR_TOO_LONG)
        totalU += feet * U_PER_FOOT
        s = s.substring(footIdx + 1).trim()
    }

    if (s.isEmpty()) {
        return if (footIdx >= 0) ParseResult.Ok(totalU)
        else ParseResult.Error(ERR_INCH_FRACTIONAL)
    }

    val tokens = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.size > 2) return ParseResult.Error(ERR_INCH_FRACTIONAL)

    var sawWhole = false
    var sawFraction = false
    for (token in tokens) {
        if (token.contains('/')) {
            if (sawFraction) return ParseResult.Error(ERR_INCH_FRACTIONAL)
            val bits = token.split('/')
            if (bits.size != 2) return ParseResult.Error(ERR_INCH_FRACTIONAL)
            val num = bits[0].toLongOrNull() ?: return ParseResult.Error(ERR_INCH_FRACTIONAL)
            val den = bits[1].toLongOrNull() ?: return ParseResult.Error(ERR_INCH_FRACTIONAL)
            if (den <= 0L) return ParseResult.Error("Denominator must be more than 0.")
            if (num > 100_000L) return ParseResult.Error(ERR_TOO_LONG)
            totalU += (num * U_PER_INCH + den / 2) / den
            sawFraction = true
        } else {
            // A whole/decimal inch part must come first and appear once.
            if (sawWhole || sawFraction) return ParseResult.Error(ERR_INCH_FRACTIONAL)
            val m = DECIMAL.matchEntire(token) ?: return ParseResult.Error(ERR_INCH_FRACTIONAL)
            when (val r = decimalToU(m.groupValues[1], m.groupValues[2], U_PER_INCH, ERR_INCH_FRACTIONAL)) {
                is ParseResult.Error -> return r
                is ParseResult.Ok -> totalU += r.valueU
            }
            sawWhole = true
        }
    }
    return ParseResult.Ok(totalU)
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatting
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Formats an internal value for display.
 *
 * Round-trip guarantee: for any value on the display grid (see [snap]),
 * `parse(format(x, sys, d), sys) == Ok(x)`. Verified by property test.
 */
fun format(valueU: Long, system: UnitSystem, denominator: Int = 16): String = when (system) {
    UnitSystem.MM -> formatDecimal(valueU, U_PER_MM, maxDecimals = 1)
    UnitSystem.CM -> formatDecimal(valueU, U_PER_CM, maxDecimals = 2)
    UnitSystem.M -> formatDecimal(valueU, U_PER_M, maxDecimals = 4)
    // 1/64" = 0.015625 exactly, so 6 places round-trips the whole grid.
    UnitSystem.INCH_DECIMAL -> formatDecimal(valueU, U_PER_INCH, maxDecimals = 6)
    UnitSystem.INCH_FRACTIONAL -> formatFractional(valueU, denominator)
}

/**
 * Formats for DISPLAY ONLY, with the unit attached: `3000 mm`, `3 m`, `12.5"`.
 *
 * 🔴 Never use this to fill a text field. [format] is the one with the
 * round-trip guarantee and the property test behind it, and it is what
 * MeasurementFieldState writes back after a commit. This function exists
 * alongside it rather than replacing it precisely so that guarantee is not
 * quietly widened to a string carrying a suffix.
 *
 * Why it exists at all: every metric length rendered as a bare number. A parts
 * row read `1800`, not `1800 mm`, and in metre mode a part read `3`. Someone who
 * already knows the job infers the unit instantly; someone opening the app for
 * the first time has to work it out, and the first thing a new user sees is the
 * seeded example job. Imperial never had the problem, because `1' 5 1/16"`
 * carries its own marks.
 *
 * WHERE IT IS USED, and the rule: a unit goes on a measurement that stands
 * ALONE and could be read wrong — a parts row, a stock row, a quick-add chip,
 * the offcut total, the infeasible warning. It is deliberately NOT used inside
 * cut-plan bar segments or the `1800 · 1200 · 900` run beneath them, where the
 * numbers are dense, the unit is already established by everything around them,
 * and at 360 dp there is no room: repeating "mm" six times across one bar would
 * cost legibility at the saw to solve a problem that is already solved.
 */
fun formatWithUnit(valueU: Long, system: UnitSystem, denominator: Int = 16): String =
    when (system) {
        UnitSystem.MM -> format(valueU, system, denominator) + " mm"
        UnitSystem.CM -> format(valueU, system, denominator) + " cm"
        UnitSystem.M -> format(valueU, system, denominator) + " m"
        // Decimal inches format as a bare number, so they need the mark.
        UnitSystem.INCH_DECIMAL -> format(valueU, system, denominator) + "\""
        // Fractional already reads 1' 5 1/16". Adding anything would be noise.
        UnitSystem.INCH_FRACTIONAL -> format(valueU, system, denominator)
    }

private fun formatDecimal(valueU: Long, perUnit: Long, maxDecimals: Int): String {
    val scale = pow10(maxDecimals)
    val scaled = (valueU * scale + perUnit / 2) / perUnit
    val whole = scaled / scale
    val frac = scaled % scale
    if (frac == 0L) return whole.toString()
    val fracStr = frac.toString().padStart(maxDecimals, '0').trimEnd('0')
    return "$whole.$fracStr"
}

private fun formatFractional(valueU: Long, denominator: Int): String {
    require(denominator in SUPPORTED_DENOMINATORS) { "Unsupported denominator: $denominator" }
    val ticksPerInch = denominator.toLong()
    val ticksPerFoot = 12L * ticksPerInch
    val unitsPerTick = U_PER_INCH / ticksPerInch

    val totalTicks = (valueU + unitsPerTick / 2) / unitsPerTick
    val feet = totalTicks / ticksPerFoot
    val remainder = totalTicks % ticksPerFoot
    val inches = remainder / ticksPerInch
    val fracTicks = remainder % ticksPerInch

    val sb = StringBuilder()
    if (feet > 0) sb.append(feet).append('\'')

    val hasInchPart = inches > 0 || fracTicks > 0
    if (hasInchPart) {
        if (sb.isNotEmpty()) sb.append(' ')
        if (inches > 0) {
            sb.append(inches)
            if (fracTicks > 0) sb.append(' ')
        }
        if (fracTicks > 0) {
            val g = gcd(fracTicks, ticksPerInch)
            sb.append(fracTicks / g).append('/').append(ticksPerInch / g)
        }
        sb.append('"')
    } else if (feet == 0L) {
        sb.append("0\"")
    }
    return sb.toString()
}

// ─────────────────────────────────────────────────────────────────────────────

private fun pow10(n: Int): Long {
    var r = 1L
    repeat(n) { r *= 10L }
    return r
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
