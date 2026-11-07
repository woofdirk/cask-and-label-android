package be.webweaver.apps.caskandlabel.core

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A tasting score: 1 to 10 stars in half-star steps, stored as half-steps (2..20) so 7.5 is exact.
 * Same contract as `CaskCore.Score` in the iOS app and `tastings.score_half_steps` on the server.
 */
@JvmInline
value class Score private constructor(val halfSteps: Int) : Comparable<Score> {
    val value: Double get() = halfSteps / 2.0

    override fun compareTo(other: Score): Int = halfSteps.compareTo(other.halfSteps)

    /** "7" or "7,5" (decimal separator of [locale]). */
    fun format(locale: Locale = Locale.getDefault()): String {
        val whole = halfSteps / 2
        return if (halfSteps % 2 == 0) "$whole" else "$whole${DecimalFormatSymbols.getInstance(locale).decimalSeparator}5"
    }

    companion object {
        const val MIN_HALF_STEPS = 2
        const val MAX_HALF_STEPS = 20

        fun ofHalfSteps(halfSteps: Int): Score? =
            if (halfSteps in MIN_HALF_STEPS..MAX_HALF_STEPS) Score(halfSteps) else null

        fun of(value: Double): Score? {
            val doubled = value * 2
            if (value < 1.0 || value > 10.0 || doubled != Math.rint(doubled)) return null
            return Score(doubled.toInt())
        }

        val all: List<Score> = (MIN_HALF_STEPS..MAX_HALF_STEPS).map { Score(it) }

        /** Average of half-step scores, formatted with one decimal ("7,3"), or null for none. */
        fun formatAverage(halfSteps: List<Int>, locale: Locale = Locale.getDefault()): String? {
            if (halfSteps.isEmpty()) return null
            val average = halfSteps.sum() / 2.0 / halfSteps.size
            val rounded = Math.round(average * 10) / 10.0
            return if (rounded == Math.rint(rounded)) "${rounded.toInt()}"
            else String.format(locale, "%.1f", rounded)
        }
    }
}
