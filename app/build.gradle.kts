import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Konfigurasi backend dibaca dari local.properties (tidak ikut di-commit).
// Bila kosong, aplikasi berjalan dalam mode offline (data hanya di perangkat).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String) = (localProps.getProperty(key) ?: "").trim()

android {
    namespace = "com.seiv.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.seiv.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.3.1"

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL").trimEnd('/')}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Ditandatangani dengan debug key agar APK release bisa langsung di-install untuk demo.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    testImplementation(libs.junit)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Firebase membawa fragment lama; ActivityResult API butuh >= 1.3.0
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
