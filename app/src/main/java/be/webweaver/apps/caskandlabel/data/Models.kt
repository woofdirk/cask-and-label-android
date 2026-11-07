package be.webweaver.apps.caskandlabel.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Rows of the shared Supabase schema (iOS repo: supabase/migrations). Column names are the contract with the
// iOS app; vocabulary values (kind, cask types, flavour profile) use the exact strings of `core.Vocabulary`.

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("adult_confirmed_at") val adultConfirmedAt: String,
)

@Serializable
data class DisplayName(@SerialName("display_name") val displayName: String)

@Serializable
data class GroupName(val name: String)

@Serializable
data class GroupMember(
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("profiles") val profile: DisplayName? = null,
)

@Serializable
data class GroupSummary(
    val id: String,
    val name: String,
    @SerialName("group_members") val members: List<GroupMember> = emptyList(),
) {
    fun isAdmin(userId: String?): Boolean = members.any { it.userId == userId && it.role == "admin" }
}

@Serializable
data class Bottle(
    val id: String,
    val name: String,
    val brand: String? = null,
    val distillery: String? = null,
    val kind: String? = null,
    val country: String? = null,
    val region: String? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    val abv: Double? = null,
    @SerialName("volume_cl") val volumeCl: Double? = null,
    @SerialName("cask_types") val caskTypes: List<String> = emptyList(),
    @SerialName("cask_details") val caskDetails: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val peated: Boolean? = null,
    @SerialName("flavor_profile") val flavorProfile: String? = null,
    @SerialName("flavor_words") val flavorWords: List<String> = emptyList(),
    @SerialName("label_photo_path") val labelPhotoPath: String? = null,
    val barcode: String? = null,
    val place: String? = null,
    val founded: Int? = null,
    val owner: String? = null,
    val website: String? = null,
    @SerialName("article_url") val articleUrl: String? = null,
    val sources: List<String> = emptyList(),
    val status: String = "pending",
    @SerialName("created_by") val createdBy: String? = null,
)

/** A new bottle as submitted to the catalogue: always `pending`, created by you. */
@Serializable
data class NewBottle(
    val id: String,
    val name: String,
    val brand: String?,
    val distillery: String?,
    val kind: String?,
    val country: String?,
    val region: String?,
    @SerialName("age_years") val ageYears: Int?,
    val abv: Double?,
    @SerialName("volume_cl") val volumeCl: Double?,
    @SerialName("cask_types") val caskTypes: List<String>,
    val features: List<String>,
    val peated: Boolean?,
    @SerialName("flavor_profile") val flavorProfile: String?,
    @SerialName("label_photo_path") val labelPhotoPath: String?,
    val status: String = "pending",
    @SerialName("created_by") val createdBy: String,
)

@Serializable
data class NewHolder(
    @SerialName("bottle_id") val bottleId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("added_by") val addedBy: String,
)

/** A tasting as written; `group_id` null means private and is always sent. */
@Serializable
data class NewTasting(
    val id: String,
    @SerialName("bottle_id") val bottleId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String?,
    @SerialName("score_half_steps") val scoreHalfSteps: Int,
    val nose: String,
    val palate: String,
    val finish: String,
    @SerialName("flavor_words") val flavorWords: List<String>,
    @SerialName("price_paid_eur") val pricePaidEur: Double?,
    @SerialName("tasted_at") val tastedAt: String,
)

/** A tasting as read, with its bottle, taster name and group name (whatever row-level security lets you see). */
@Serializable
data class TastingRow(
    val id: String,
    @SerialName("bottle_id") val bottleId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("score_half_steps") val scoreHalfSteps: Int,
    val nose: String? = null,
    val palate: String? = null,
    val finish: String? = null,
    @SerialName("flavor_words") val flavorWords: List<String> = emptyList(),
    @SerialName("price_paid_eur") val pricePaidEur: Double? = null,
    @SerialName("tasted_at") val tastedAt: String,
    @SerialName("bottles") val bottle: Bottle? = null,
    @SerialName("profiles") val profile: DisplayName? = null,
    @SerialName("groups") val group: GroupName? = null,
)
