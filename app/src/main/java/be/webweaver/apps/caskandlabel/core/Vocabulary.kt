package be.webweaver.apps.caskandlabel.core

/**
 * Whisky terms for pick lists and filters, identical to `CaskCore.Vocabulary` in the iOS app (stored as shown,
 * so both apps write the same values to the server). Names only, no shop data.
 */
object Vocabulary {
    val kinds = listOf(
        "Single Malt", "Blended", "Bourbon Whiskey", "Tennessee Whiskey", "Blended Malt", "Grain Whisky",
        "Rye Whisky", "Pot Still",
    )

    val countries = listOf(
        "Schotland", "Ierland", "Amerika", "Japan", "Canada", "België", "India", "Taiwan", "Nederland", "Wales",
        "China", "Denemarken", "Israël",
    )

    val scotchRegions = listOf("Islay", "Highland", "Speyside", "Islands", "Lowland", "Campbeltown")

    val flavorProfiles = listOf("Licht & Bloemig", "Fruitig & Kruidig", "Vol & Rond", "Krachtig & Rokerig")

    val caskTypes = listOf(
        "Bourbon Cask", "Port Cask", "Rum Cask", "Sherry Cask", "Wine Cask", "Calvados Cask", "Cognac Cask",
        "Beer Cask", "Brandy Cask", "Madeira Cask", "Manzanilla Cask", "Marsala Cask", "Sake Cask", "Tequila Cask",
    )

    val caskDetails = listOf(
        "Oloroso Cask Finish", "Pedro Ximenez Finish", "Amarone Wine Finish", "Bordeaux Wine Finish",
        "Madeira Finish", "Moscatel Cask Finish", "Sauternes Finish",
    )

    val features = listOf(
        "Cask Strength", "Limited Release", "Non Chill Filtered", "Single Cask", "Small Batch", "Double Matured",
        "Organic",
    )

    /** Flavour tags, per bottle (from the label) and per tasting. */
    val flavorWords = listOf(
        "rokerig", "turf", "fruitig", "citrus", "gedroogd fruit", "bloemig", "kruidig", "peper",
        "zoet", "vanille", "honing", "karamel", "chocolade", "nootachtig", "houtig", "zilt", "medicinaal",
    )

    /** Suggested flavour profile; null when nothing points one way. Same rules as the iOS app. */
    fun flavorProfile(peated: Boolean?, caskTypes: List<String>, flavorWords: List<String>): String? {
        val words = flavorWords.toSet()
        if (peated == true || words.any { it in setOf("rokerig", "turf", "medicinaal") }) return "Krachtig & Rokerig"
        if (caskTypes.any { it in setOf("Sherry Cask", "Wine Cask", "Port Cask") } ||
            words.any { it in setOf("gedroogd fruit", "chocolade") }
        ) return "Vol & Rond"
        if (words.any { it in setOf("fruitig", "citrus", "kruidig", "peper") }) return "Fruitig & Kruidig"
        if ("bloemig" in words) return "Licht & Bloemig"
        return null
    }
}
