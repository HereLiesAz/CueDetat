// PoolProtractor/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = 36

    namespace = "com.hereliesaz.cuedetat" // Correct position
    buildToolsVersion = "36.0.0" // Correct position

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 26
        targetSdk = 36
        versionCode = 3 // THIS IS YOUR APP'S CURRENT VERSION CODE
        versionName = "0.3.2" // THIS IS YOUR APP'S CURRENT VERSION NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // These blocks should be direct children of the 'android' block
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    // sourceSets block also belongs here, directly under 'android'
    sourceSets {
        getByName("main") {
            // Your custom source set configurations go here if any.
            // It should be empty if there are no custom source set configurations.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

// The dependencies block *must* be outside the 'android' block,
// and directly within the 'app' module's build.gradle.kts file.
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.graphics)
    // CameraX dependencies (ensure these are up-to-date and compatible)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlin.stdlib)

    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(platform(libs.androidx.compose.bom)) // Should only appear once for the BOM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.activity.compose)

    implementation(libs.object1.detection) // ML Kit Object Detection Dependency

    // For GitHubUpdater - OkHttp for network requests, and JSON parsing
    implementation(libs.okhttp) // Using a recent stable version
    implementation(libs.json) // For JSONObject
}