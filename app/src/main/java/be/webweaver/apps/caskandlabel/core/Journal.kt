package be.webweaver.apps.caskandlabel.core

import java.time.Instant

/** What the journal needs of one tasting; built from server rows so the logic stays testable. */
data class JournalTasting(
    val id: String,
    val bottleId: String,
    val bottleTitle: String,
    val userId: String,
    val groupId: String?,
    val scoreHalfSteps: Int,
    val tastedAt: Instant,
    val labelPhotoPath: String? = null,
    val flavorProfile: String? = null,
    val caskTypes: List<String> = emptyList(),
)

/** One row per bottle (spec 0.12): average score, number of tastings and the latest tasting. */
data class JournalRow(
    val bottleId: String,
    val title: String,
    val labelPhotoPath: String?,
    val flavorProfile: String?,
    val caskTypes: List<String>,
    val scores: List<Int>,
    val lastTastedAt: Instant,
    val groupIds: Set<String?>,
) {
    val count: Int get() = scores.size
    val averageHalfSteps: Double get() = scores.average()
}

/** Which tastings the journal shows: everything, only private ones, or one group's. */
sealed interface GroupScope {
    data object All : GroupScope
    data object Private : GroupScope
    data class Group(val id: String) : GroupScope
}

enum class JournalSort { DATE, SCORE, NAME }

object Journal {
    fun rows(
        tastings: List<JournalTasting>,
        scope: GroupScope = GroupScope.All,
        query: String = "",
        sort: JournalSort = JournalSort.DATE,
    ): List<JournalRow> {
        val needle = query.trim()
        val rows = tastings
            .filter { t ->
                when (scope) {
                    GroupScope.All -> true
                    GroupScope.Private -> t.groupId == null
                    is GroupScope.Group -> t.groupId == scope.id
                }
            }
            .filter { needle.isEmpty() || it.bottleTitle.contains(needle, ignoreCase = true) }
            .groupBy { it.bottleId }
            .map { (bottleId, list) ->
                val latest = list.maxBy { it.tastedAt }
                JournalRow(
                    bottleId = bottleId,
                    title = latest.bottleTitle,
                    labelPhotoPath = list.firstNotNullOfOrNull { it.labelPhotoPath },
                    flavorProfile = latest.flavorProfile,
                    caskTypes = latest.caskTypes,
                    scores = list.map { it.scoreHalfSteps },
                    lastTastedAt = latest.tastedAt,
                    groupIds = list.map { it.groupId }.toSet(),
                )
            }
        return when (sort) {
            JournalSort.DATE -> rows.sortedByDescending { it.lastTastedAt }
            JournalSort.SCORE -> rows.sortedWith(compareByDescending<JournalRow> { it.averageHalfSteps }.thenByDescending { it.lastTastedAt })
            JournalSort.NAME -> rows.sortedBy { it.title.lowercase() }
        }
    }
}
