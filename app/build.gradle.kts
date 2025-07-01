plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
kotlin {
    jvmToolchain(17)

}

dependencies {

    implementation(libs.androidx.xr.scenecore)
    // Required for Java
    // Required for Kotlin
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.sceneform.base)
    implementation(libs.androidx.graphics.shapes)

    // Use to write unit tests
    testImplementation(libs.androidx.scenecore.testing)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Jetpack XR
    implementation(libs.androidx.xr.compose.material3)
    implementation(libs.androidx.xr.compose)
    implementation(libs.androidx.xr.scenecore)
    implementation(libs.androidx.xr.runtime)
    implementation(libs.androidx.xr.arcore)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // ARCore
    implementation(libs.arcore.client)

    // Android XR Dependencies (New)
    val xrVersion = "1.0.0-alpha04"
    implementation("androidx.xr.scenecore:scenecore:$xrVersion")
    implementation("androidx.xr.compose:compose:$xrVersion")
    implementation("androidx.xr.runtime:runtime-openxr:$xrVersion")
    implementation("androidx.xr.arcore:arcore:$xrVersion")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.xr.runtime)

    // Use in environments that do not support OpenXR
    testImplementation(libs.androidx.runtime.testing)
}