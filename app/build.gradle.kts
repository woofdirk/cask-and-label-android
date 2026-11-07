import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Public project values; the publishable key comes from local.properties (git ignores it), like
// Secrets.xcconfig in the iOS app. Without a key the app shows a "not configured" screen.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val supabaseProjectRef = localProperties.getProperty("supabase.projectRef") ?: "dccuodnhizifzwnnvchb"
val supabasePublishableKey = localProperties.getProperty("supabase.publishableKey") ?: ""

android {
    namespace = "be.webweaver.apps.caskandlabel"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "be.webweaver.apps.caskandlabel"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        buildConfigField("String", "SUPABASE_URL", "\"https://$supabaseProjectRef.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    testImplementation(libs.junit)
}
