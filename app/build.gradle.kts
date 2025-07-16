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
        versionCode = 20
        versionName = "0.7.7.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
            ndk.abiFilters.addAll(listOf("arm64-v8a"))

        }
        multiDexEnabled = true
        signingConfig = signingConfigs.getByName("debug")
    }
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    splits {
        abi {
            reset() // Clear any existing ABI configurations
            include("arm64-v8a")
            // exclude "x86", "x86_64" // Exclude unwanted ABIs
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            multiDexEnabled = true
            //useLegacyPackaging = false
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            multiDexEnabled = true
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("G:\\My Drive\\az_apk_keystore.jks")
            storePassword = "18187077190901818"
            keyAlias = "key0"
            keyPassword = "18187077190901818"
        }
        create("release") { // It's good practice to use create for release if it's not already defined elsewhere
            val userHome = System.getProperty("user.home")
            val tmpFilePath = "$userHome/work/_temp/keystore/"
            val tmpDir = File(tmpFilePath)
            val allFilesFromDir = tmpDir.listFiles()

            if (allFilesFromDir != null && allFilesFromDir.isNotEmpty()) {
                val keystoreFile = allFilesFromDir.first()
                val destinationDir = File(project.projectDir, "keystore")
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs()
                }
                keystoreFile.renameTo(File(destinationDir, "your_keystore.jks"))
            }

            storeFile = file("keystore/your_keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
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
        jvmTarget = "17" // Match this with sourceCompatibility and targetCompatibility
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
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13599879 rc2"

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
        implementation(libs.androidx.material3)


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
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
        implementation(libs.material)
        implementation(libs.androidx.material.icons.extended)

        implementation(libs.material3)

        // Declare individual CameraX library dependencies without versions
        implementation(libs.androidx.camera.core)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle)
        implementation(libs.androidx.camera.view)
        androidTestImplementation(libs.androidx.junit)
        testImplementation(libs.junit)
        implementation(libs.androidx.animation.core)

        // OpenCV for Computer Vision

    }
}
dependencies {
    implementation(libs.androidx.ui.graphics)
    implementation(project(":opencv"))
    // CameraX core and required modules
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.room.ktx)
    val cameraxVersion = "1.4.2" // Use a recent stable version

    // The ImageAnalysis use case specifically
    implementation(libs.androidx.camera.video)

    implementation(libs.object1.detection.custom)
    implementation(libs.vision.common)

    // TensorFlow Lite Task Vision library for custom models
    implementation(libs.tensorflow.lite.task.vision)

    // Retrofit for GitHub API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    implementation(libs.object1.detection.custom)
    implementation(libs.object1.detection)
    implementation(libs.vision.common)

    androidTestImplementation (libs.androidx.junit) // Or the latest version shown by Android Studio's lint
    androidTestImplementation (libs.androidx.espresso.core)


}



kapt {
    correctErrorTypes = true
}
