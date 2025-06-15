plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "3.4.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // This API is deprecated but is the most straightforward way to rename only APKs.
    // It will be removed in a future version of the Android Gradle Plugin.
    @Suppress("DEPRECATION")
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this
            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                output.outputFileName =
                    "CueDetat-${variant.versionName}-${variant.buildType.name}.apk"
            }
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
        compose = true
        buildConfig = true
    }
    composeOptions {
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.compose.material3)

        // Hilt for Dependency Injection
        implementation(libs.hilt.android)
        kapt(libs.hilt.compiler)
        implementation(libs.androidx.hilt.navigation.compose)

        // CameraX for Camera Preview
        implementation(libs.androidx.camera.core)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle)
        implementation(libs.androidx.camera.view)

        // Retrofit for network calls
        implementation(libs.retrofit)
        implementation(libs.converter.gson)

        // Palette API for dynamic colors
        implementation(libs.androidx.palette)

        // NEW: Added for lifecycle-aware composition
        implementation(libs.androidx.lifecycle.runtime.compose)
        // NEW: Added for ExperimentalMaterial3ExpressiveApi used in VerticalSlider

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
        implementation(libs.material)
        implementation(libs.androidx.material.icons.extended)

    }
}
dependencies {
    implementation(libs.ui)
}

kapt {
    correctErrorTypes = true
}
