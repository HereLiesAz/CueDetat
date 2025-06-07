plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)

}

android {
    signingConfigs {
        create("release") {
            storeFile = file("G:\\My Drive\\az_apk_keystore.jks")
            storePassword = "18187077190901818"
        }
    }
    compileSdk = 36 // Or your current target SDK

    defaultConfig {
        applicationId = "com.hereliesaz.poolprotractor"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationIdSuffix = "trackless"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            applicationIdSuffix = "trackless"
        }
        getByName("debug") {
            applicationIdSuffix = "trackless"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true // Enable Compose
        viewBinding = true

    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Example: Use the version compatible with your Kotlin plugin
    }

    namespace = "com.hereliesaz.poolprotractor"
}

dependencies {
    implementation(libs.androidx.core.ktx) // Example of a Kotlin dependency if you mix
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.graphics) // Or the specific version catalog alias if you have one
    // CameraX dependencies
    implementation(libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // ... other dependencies
    implementation(libs.androidx.activity.compose) // Or the latest version
    implementation(platform(libs.androidx.compose.bom)) // Or the latest BOM
    implementation(libs.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    // ... other compose dependencies
}