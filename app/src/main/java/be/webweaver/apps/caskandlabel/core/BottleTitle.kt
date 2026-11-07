package be.webweaver.apps.caskandlabel.core

/**
 * The name to show for a bottle: its name, plus the age when the name does not already contain it
 * ("Glenmorangie The Lasanta 15 jaar"). Mirrors `CaskCore.BottleTitle`.
 */
object BottleTitle {
    fun title(name: String, ageYears: Int?, ageLabel: (Int) -> String): String {
        if (ageYears == null || containsNumber(ageYears, name)) return name
        return "$name ${ageLabel(ageYears)}"
    }

    /** True when [number] stands on its own in [text] ("Lasanta 15", "15yo"), not inside another number ("2015"). */
    fun containsNumber(number: Int, text: String): Boolean =
        Regex("(?<![0-9])$number(?![0-9])").containsMatchIn(text)
}
