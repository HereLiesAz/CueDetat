import java.io.FileInputStream
import java.util.Properties


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.compose)
    id("com.github.triplet.play")

}

kotlin {
    jvmToolchain(17)
}

// Load properties from local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {

    namespace = libs.versions.applicationId.get().toString()
    compileSdk = 36

    defaultConfig {
        applicationId = libs.versions.applicationId.get().toString()
        minSdk = 26
        targetSdk = 36
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get().toString()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        //ndk.abiFilters.addAll(listOf("arm64-v8a"))
        multiDexEnabled = true
    }

    splits {
        abi {
            isEnable = true // Activates ABI splitting
            reset() // Ensures only explicitly included ABIs are considered
            include(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            ) // These are the four common Android ABIs
            isUniversalApk =
                false // Crucial: Set to false to prevent a fifth "universal" APK that contains all ABIs. If you want a universal APK in addition to the four, set this to true.
        }
    }

    // FIX: Moved packaging rules to the stable, top-level packaging block.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        // Exclude duplicate license files from dependencies to prevent build failures.
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
    // Play Publisher configuration block


    // Optional: Manage other aspects like metadata, images, etc.
    // For full details, refer to the Gradle Play Publisher documentation.
    signingConfigs {
        create("release") {
            // Read signing information from local.properties
            val storeFile = localProperties.getProperty("signing.store.file")
            if (storeFile != null) {
                this.storeFile = file(storeFile)
                this.storePassword = localProperties.getProperty("signing.store.password")
                this.keyAlias = localProperties.getProperty("signing.key.alias")
                this.keyPassword = localProperties.getProperty("signing.key.password")
            }
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
            signingConfig = signingConfigs.getByName("release")
            multiDexEnabled = true
        }
        getByName("debug") {
            isMinifyEnabled = false
            multiDexEnabled = true
        }
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13599879 rc2"


}

//play {
//    jsonFile =
//        file("\"G:\\My Drive\\cue-detat-466601-50675aa2e046.json\"") // e.g., file("play-credentials.json")
//    track = "internal" // Or "alpha", "beta", "production"
//    publishNewApp.set(false) // Use with caution, typically set to false after first upload
//    userFraction.set(1.0)
//    releaseName.set("${appId}-${buildTypeName}-${abi}-${versionName}-${versionCode}")
//    releaseStatus.set(com.github.triplet.gradle.play.api.ReleaseStatus.COMPLETED)
//}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3) // Consolidating material3 dependencies
    implementation(libs.material)
    implementation(libs.androidx.material.icons.extended)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.room.ktx)
    implementation(libs.glance)
    implementation(libs.sceneform.base)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Retrofit for network calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Palette API for dynamic colors
    implementation(libs.androidx.palette)

    // Lifecycle-aware composition
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Animation
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.kphysics)

    // DataStore for persisting user preferences
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.rxjava2) // optional
    implementation(libs.androidx.datastore.preferences.rxjava3) // optional

    // OpenCV for Computer Vision
    implementation(libs.opencv)

    // ML Kit & TensorFlow
    implementation(libs.object1.detection.custom)
    implementation(libs.vision.common)
    implementation(libs.tensorflow.lite.task.vision)
    implementation(libs.object1.detection)



    implementation(libs.kphysics)

    // DataStore for persisting user preferences
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.rxjava2) // optional
    implementation(libs.androidx.datastore.preferences.rxjava3) // optional

}

kapt {
    correctErrorTypes = true
}
//
// --- Custom Artifact Renaming ---
// The new Android Gradle Plugin versions (8.x+) no longer allow direct renaming of output files.
// The official way to get custom-named files is to create a task that copies the final artifacts
// and renames them. This block does that, placing the final files in the `build/dist/` directory.
androidComponents {
    onVariants { variant ->
        // Determine the source of the artifacts based on the packaging type
        val artifacts = if (variant.buildType == "release") {
            variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.BUNDLE)
        } else {
            variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)
        }

        // Register a new Copy task for each vasriant
        tasks.register<Copy>("copyAndRename_${variant.name}") {
            group = "Distribution"
            description = "Copies and renames artifacts for the ${variant.name} variant."

            from(artifacts)
            into(project.layout.buildDirectory.dir("dist"))

            // This closure is called for each file as it's copied
            // ... inside tasks.register<Copy>("copyAndRename_${variant.name}")
            rename { fileName ->
                val appId = variant.applicationId.get()

                // FIX: Access versionName and versionCode from the variant's output.
                val mainOutput = variant.outputs.first()
                val versionName = mainOutput.versionName.get() ?: "unknown"
                val versionCode = mainOutput.versionCode.get()

                val buildTypeName = variant.buildType

                if (fileName.endsWith(".aab")) {
                    // For App Bundles, the ABI is always "universal"
                    "${appId}-${buildTypeName}-universal-${versionName}-${versionCode}.aab"
                } else {
                    // For APKs, we parse the ABI from the default filename
                    val abiRegex = "-((armeabi-v7a)|(arm64-v8a)|(x86)|(x86_64))".toRegex()
                    val abi = abiRegex.find(fileName)?.groups?.get(1)?.value ?: "universal"

                    "${appId}-${buildTypeName}-${abi}-${versionName}-${versionCode}.apk"
                }
            }
        }
    }
}