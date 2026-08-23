import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin Multiplatform. No Android, OpenCV or ARCore types may enter this
// module — that is what makes it testable with `:core:advisor:jvmTest` on any
// machine, with no SDK and no emulator.
//
// The Android target is registered only when an SDK is actually available, so a
// contributor (or a sandbox) without one can still build and test the core.
val androidSdkPresent: Boolean =
    providers.environmentVariable("ANDROID_HOME").isPresent ||
        providers.environmentVariable("ANDROID_SDK_ROOT").isPresent ||
        rootProject.file("local.properties").exists()

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

if (androidSdkPresent) {
    apply(plugin = "com.android.library")
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    if (androidSdkPresent) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:units"))
            api(project(":core:geometry"))
            api(project(":core:physics"))
            api(project(":core:aim"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

if (androidSdkPresent) {
    extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
        namespace = "com.hereliesaz.cuedetat.core.advisor"
        compileSdk = 37
        defaultConfig { minSdk = 29 }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
}
