package be.webweaver.apps.caskandlabel.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

/**
 * Every server call, made as the signed-in user; row-level security decides what comes back
 * (a member of group A never sees group B). Same queries as the iOS app (Groups.swift, AccountSync.swift).
 */
object Repository {
    const val BUCKET = "bottle-photos"

    private val client get() = Backend.requireClient()

    private const val TASTING_COLUMNS = """
        id, bottle_id, user_id, group_id, score_half_steps, nose, palate, finish, flavor_words, price_paid_eur,
        tasted_at, bottles(*), profiles(display_name), groups(name)
    """

    val currentUserId: String? get() = Backend.client?.auth?.currentUserOrNull()?.id
    val currentEmail: String? get() = Backend.client?.auth?.currentUserOrNull()?.email

    // Account

    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
        signedUrls.clear()
    }

    suspend fun myProfile(): ProfileRow? {
        val id = currentUserId ?: return null
        return client.from("profiles").select { filter { eq("id", id) } }.decodeList<ProfileRow>().firstOrNull()
    }

    /** The profile holds the display name group mates see and when you confirmed being 18+. */
    suspend fun createProfile(displayName: String) {
        val id = currentUserId ?: return
        client.from("profiles").insert(ProfileRow(id, displayName, Instant.now().toString()))
    }

    suspend fun renameProfile(displayName: String) {
        val id = currentUserId ?: return
        client.from("profiles").update({ set("display_name", displayName) }) { filter { eq("id", id) } }
    }

    // Groups

    suspend fun groups(): List<GroupSummary> =
        client.from("groups")
            .select(Columns.raw("id, name, group_members(user_id, role, profiles(display_name))")) {
                order("name", Order.ASCENDING)
            }
            .decodeList()

    suspend fun createGroup(name: String) {
        client.postgrest.rpc("create_group", buildJsonObject { put("p_name", name) })
    }

    suspend fun createInvite(groupId: String): String =
        client.postgrest.rpc("create_invite", buildJsonObject { put("p_group", groupId) }).decodeAs<String>()

    /** Admins revoke a code (iOS 0.32): it stops working for anyone who has not joined yet. */
    suspend fun revokeInvite(code: String) {
        client.from("group_invites").update({ set("revoked_at", Instant.now().toString()) }) { filter { eq("code", code) } }
    }

    suspend fun joinWithCode(code: String) {
        client.postgrest.rpc("accept_invite_code", buildJsonObject { put("p_code", code) })
    }

    /** Your tastings in the group become private again (same choice as the iOS app). */
    suspend fun leaveGroup(groupId: String) {
        client.postgrest.rpc("leave_group", buildJsonObject {
            put("p_group", groupId)
            put("p_keep_as_private", true)
        })
    }

    // Tastings

    /** Everything you may see: your own tastings (all groups and private) and those shared with your groups. */
    suspend fun visibleTastings(): List<TastingRow> =
        client.from("tastings")
            .select(Columns.raw(TASTING_COLUMNS)) { order("tasted_at", Order.DESCENDING) }
            .decodeList()

    suspend fun groupTastings(groupId: String): List<TastingRow> =
        client.from("tastings")
            .select(Columns.raw(TASTING_COLUMNS)) {
                filter { eq("group_id", groupId) }
                order("tasted_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun bottleTastings(bottleId: String): List<TastingRow> =
        client.from("tastings")
            .select(Columns.raw(TASTING_COLUMNS)) {
                filter { eq("bottle_id", bottleId) }
                order("tasted_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun deleteTasting(id: String) {
        client.from("tastings").delete { filter { eq("id", id) } }
    }

    suspend fun addTasting(tasting: NewTasting) {
        client.from("tastings").insert(tasting)
    }

    // Bottles

    suspend fun bottle(id: String): Bottle? =
        client.from("bottles").select { filter { eq("id", id) } }.decodeList<Bottle>().firstOrNull()

    /** Bottles you may see (approved, your own, or tasted in your groups) whose name or brand matches. */
    suspend fun searchBottles(query: String): List<Bottle> =
        client.from("bottles")
            .select {
                val q = query.trim().replace(Regex("[,()%*]"), " ")
                if (q.isNotEmpty()) filter { or { ilike("name", "%$q%"); ilike("brand", "%$q%") } }
                order("name", Order.ASCENDING)
                limit(50)
            }
            .decodeList()

    /**
     * Submits a bottle (status `pending`, only visible to you and the groups you taste it in until the catalogue
     * admin approves it), with you as holder ("Fles van") and its front-label photo at
     * `<user-id>/<bottle-id>/front.jpg`. The row goes first: others may only read files of bottles they can see.
     */
    suspend fun addBottle(draft: BottleDraft, labelJpeg: ByteArray?): String {
        val user = currentUserId ?: error("not signed in")
        val id = UUID.randomUUID().toString()
        val photoPath = labelJpeg?.let { "$user/$id/front.jpg" }
        client.from("bottles").insert(draft.toRow(id, user, photoPath))
        runCatching { client.from("bottle_holders").insert(NewHolder(id, user, user)) }
        if (labelJpeg != null && photoPath != null) {
            client.storage.from(BUCKET).upload(photoPath, labelJpeg) {
                upsert = true
                contentType = ContentType.Image.JPEG
            }
        }
        return id
    }

    // Photos (private bucket: signed URLs, cached for a while)

    private val signedUrls = ConcurrentHashMap<String, Pair<String, Instant>>()

    suspend fun photoUrl(path: String): String? {
        signedUrls[path]?.let { (url, until) -> if (until.isAfter(Instant.now())) return url }
        return runCatching {
            client.storage.from(BUCKET).createSignedUrl(path, 1.hours)
        }.getOrNull()?.also { signedUrls[path] = it to Instant.now().plusSeconds(50 * 60) }
    }

    fun newId(): String = UUID.randomUUID().toString()
}

/** The bottle form's values before submitting. */
data class BottleDraft(
    val name: String,
    val brand: String? = null,
    val distillery: String? = null,
    val kind: String? = null,
    val country: String? = null,
    val region: String? = null,
    val ageYears: Int? = null,
    val abv: Double? = null,
    val volumeCl: Double? = null,
    val caskTypes: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val peated: Boolean? = null,
    val flavorProfile: String? = null,
) {
    fun toRow(id: String, user: String, labelPhotoPath: String?) = NewBottle(
        id = id, name = name, brand = brand, distillery = distillery, kind = kind, country = country, region = region,
        ageYears = ageYears, abv = abv, volumeCl = volumeCl, caskTypes = caskTypes, features = features,
        peated = peated, flavorProfile = flavorProfile, labelPhotoPath = labelPhotoPath, createdBy = user,
    )
}
