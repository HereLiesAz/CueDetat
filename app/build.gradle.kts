import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 26
        targetSdk = 36
        val versionProps = Properties()
        versionProps.load(rootProject.file("version.properties").reader())
        versionCode = (project.findProperty("versionCode") as? String
            ?: versionProps.getProperty("versionCode")).toInt()
        versionName = (project.findProperty("versionName") as? String
            ?: versionProps.getProperty("versionName"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Migrated from kotlinOptions to compilerOptions
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.datastore.preferences)

    // Core & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.androidx.hilt.navigation.compose)

    // CameraX
    implementation(libs.bundles.camera)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.aznavrail)

    // Location
    implementation(libs.play.services.location)

    // Computer Vision
    implementation(libs.mlkit)
    implementation(libs.opencv)

    // ARCore — Depth API (optional; app degrades gracefully on unsupported devices)
    implementation("com.google.ar:core:1.44.0")

    // TFLite — pocket detection model
    implementation(libs.tensorflow.lite)

    // Physics
    // implementation(libs.google.liquidfun)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // implementation("cljsjs:liquidfun:1.1.0-0")
    implementation(libs.kotlin.metadata.jvm)

    constraints {
        implementation("io.netty:netty-codec-http2:4.1.124.Final") {
            because("Transitive dependency vulnerabilities in testing/grpc")
        }
        implementation("com.google.guava:guava:32.0.0-android") {
            because("Transitive dependency vulnerabilities in various plugins")
        }
        implementation("org.bitbucket.b_c:jose4j:0.9.6") {
            because("Transitive dependency vulnerability")
        }
    }
}
