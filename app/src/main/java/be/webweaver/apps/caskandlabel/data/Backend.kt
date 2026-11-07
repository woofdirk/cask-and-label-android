package be.webweaver.apps.caskandlabel.data

import be.webweaver.apps.caskandlabel.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/** The one Supabase client (Frankfurt project shared with the iOS app). Null without a publishable key. */
object Backend {
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_") ||
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("eyJ")

    val client: SupabaseClient? by lazy {
        if (!isConfigured) return@lazy null
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
            install(Auth) {
                scheme = "caskandlabel"
                host = "login-callback"
            }
            install(Postgrest)
            install(Storage)
        }
    }

    fun requireClient(): SupabaseClient = client ?: error("Supabase is not configured")
}
