import java.util.Properties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val versionPropsFile = rootProject.file("version.properties")
val versionPropsPath = versionPropsFile.absolutePath
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val playPublicRsa = localProperties.getProperty("PLAY_PUBLIC_RSA") ?: ""

var majorVal = (versionProps.getProperty("MAJOR") ?: "0").toInt()
var minorVal = (versionProps.getProperty("MINOR") ?: "0").toInt()
var patchVal = (versionProps.getProperty("PATCH") ?: "0").toInt()
var buildVal = (versionProps.getProperty("BUILD") ?: "0").toInt()
val lastMajorVal = (versionProps.getProperty("LAST_MAJOR") ?: majorVal.toString()).toInt()
val lastMinorVal = (versionProps.getProperty("LAST_MINOR") ?: minorVal.toString()).toInt()

val isBuildingTask = gradle.startParameter.taskNames.any {
    it.contains("assemble") || it.contains("bundle") || it.contains("install")
}

if (isBuildingTask) {
    buildVal++
    if (majorVal != lastMajorVal || minorVal != lastMinorVal) {
        patchVal = 0
    } else {
        patchVal++
    }
}

val finalBuild = buildVal
val finalPatch = patchVal
val finalMajor = majorVal
val finalMinor = minorVal
val finalVersionName = "$finalMajor.$finalMinor.$finalPatch.$finalBuild"
val finalIsBuilding = isBuildingTask

plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Removed for AGP 9.0 built-in Kotlin
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 29
        targetSdk = 36
        
        versionCode = finalBuild
        versionName = finalVersionName

        // Task to write back the updated properties
        tasks.register("updateVersionProperties") {
            val path = versionPropsPath
            val b = finalBuild
            val p = finalPatch
            val maj = finalMajor
            val min = finalMinor
            val vn = finalVersionName
            val shouldRun = finalIsBuilding

            doLast {
                if (shouldRun) {
                    val properties = Properties()
                    val f = File(path)
                    if (f.exists()) {
                        FileInputStream(f).use { properties.load(it) }
                    }
                    properties.setProperty("BUILD", b.toString())
                    properties.setProperty("PATCH", p.toString())
                    properties.setProperty("LAST_MAJOR", maj.toString())
                    properties.setProperty("LAST_MINOR", min.toString())
                    properties.setProperty("versionCode", b.toString())
                    properties.setProperty("versionName", vn)
                    FileOutputStream(f).use { properties.store(it, "Automated Version Update") }
                }
            }
        }
        
        // Ensure the update happens on every relevant build
        tasks.matching { it.name.contains("assemble") || it.name.contains("bundle") || it.name.contains("install") }.all {
            dependsOn("updateVersionProperties")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "PLAY_PUBLIC_RSA", "\"$playPublicRsa\"")
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
        mlModelBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "**/libc++_shared.so"
        }
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
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.support)
    ksp(libs.hilt.compiler)
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

    // Meta Wearables DAT
    implementation(libs.mwdat.core)
    implementation(libs.mwdat.camera)
    debugImplementation(libs.mwdat.mockdevice)

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
        implementation("org.bitbucket.b_c:jose4j:0.9.6") {
            because("Transitive dependency vulnerability")
        }
        implementation("com.google.guava:guava:32.1.3-android") {
            because("Transitive dependency vulnerability")
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
            useVersion("4.1.132.Final")
            because("Transitive dependency vulnerabilities in testing/grpc")
        }
    }
}
