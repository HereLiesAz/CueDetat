plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13599879 rc2"
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Jetpack XR
    implementation(libs.androidx.xr.compose.material3)
    implementation(libs.androidx.xr.compose)
    implementation(libs.androidx.xr.scenecore)
    implementation(libs.androidx.xr.runtime)
    implementation(libs.androidx.xr.arcore)
    implementation(libs.kotlinx.coroutines.guava)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.benchmark.common)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Math
    implementation(libs.kotlin.math)

    // Testing - moved from implementation
    testImplementation(libs.androidx.room.compiler.processing.testing)
    testImplementation(libs.androidx.benchmark.common)

    implementation(libs.androidx.xr.scenecore)
    implementation(libs.androidx.xr.arcore)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // Jetpack XR Libraries
    implementation("androidx.xr.compose:compose:1.0.0-alpha04")
    implementation("androidx.xr:xr-scenecore:1.0.0-alpha01")
    implementation("androidx.xr:xr-arcore:1.0.0-alpha01")
    implementation("androidx.vectordrawable:vectordrawable-seekable:1.0.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("androidx.xr.scenecore:scenecore:1.0.0-alpha04")
    // Required for Java
    implementation("com.google.guava:listenablefuture:1.0")
    // Required for Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // Use to write unit tests
    testImplementation("androidx.xr.scenecore:scenecore-testing:1.0.0-alpha04")
    implementation("androidx.xr.runtime:runtime:1.0.0-alpha04")

    // Use in environments that do not support OpenXR
    testImplementation("androidx.xr.runtime:runtime-testing:1.0.0-alpha04")
    implementation("androidx.xr.compose.material3:material3:1.0.0-alpha08")
    implementation("androidx.xr.compose:compose:1.0.0-alpha04")

    // Use to write unit tests
    testImplementation("androidx.xr.compose:compose-testing:1.0.0-alpha04")
    implementation("androidx.xr.arcore:arcore:1.0.0-alpha04")


}