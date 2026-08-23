import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin Multiplatform. No Android, OpenCV or ARCore types may enter this
// module — that is what makes it testable with `:core:units:jvmTest` on any
// machine, with no SDK and no emulator.
//
// There is deliberately NO Android target here. The module has no Android source
// set to build, and Android consumers resolve the `jvm()` variant through
// Kotlin's standard jvm -> androidJvm compatibility rule. An earlier revision
// registered androidTarget() behind an "is the SDK present?" check; that check
// passed locally (no SDK, so it was skipped) and failed on CI (SDK present, so
// it applied 'com.android.library', which AGP 9 refuses to load alongside the
// multiplatform plugin). A conditional that changes what gets built depending on
// the machine is not a portability feature, it is a hidden second configuration.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {

        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
