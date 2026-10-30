import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Versioning. Read-only view of the same uppercase keys app/build.gradle.kts reads from
// version.properties (MAJOR/MINOR/PATCH/BUILD); this module never writes the file.
// The watch APK shares the phone's applicationId, and Play requires every APK under one
// applicationId to carry a distinct versionCode, so the wear code is the phone's BUILD plus
// WEAR_VERSION_CODE_OFFSET. The offset keeps the two sequences disjoint as long as the phone's
// versionCode (BUILD, commit count, or the Play workflow's -PversionCodeOverride) stays below it.
// Every :app assemble* task builds :wear:assembleDebug (app/build.gradle.kts), but no workflow
// publishes :wear.
val wearVersionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun wearVersionKey(key: String) = wearVersionProps.getProperty(key)?.trim()?.toIntOrNull() ?: 0
val WEAR_VERSION_CODE_OFFSET = 1_000_000_000
val wearBuild = wearVersionKey("BUILD")

android {
    namespace = "com.hereliesaz.cuedetat.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 30
        targetSdk = 37
        versionCode = WEAR_VERSION_CODE_OFFSET + wearBuild
        versionName = "${wearVersionKey("MAJOR")}.${wearVersionKey("MINOR")}.${wearVersionKey("PATCH")}.$wearBuild"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.play.services.wearable)

    // Compose for Wear OS
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)

    // Core & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
}
