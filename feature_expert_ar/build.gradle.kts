// On-demand dynamic feature module: carries the Expert-only ARCore table-scan
// flow (ArTableSession / ArFrameProcessor / ArCoreBackground / TableScanScreen /
// TableScanViewModel / TableScanAnalyzer + ArControllerImpl) AND the ARCore
// dependency out of the base install.
//
// Unlike :feature_mlmodel (asset-only), this module ships Kotlin + Compose code.
// The base (:app) never references these classes at compile time — it talks to
// the `ArController` interface and loads `ArControllerImpl` via reflection after
// the split installs (see ArControllerFacade / PlayArFeatureDelivery). Delivery
// is entitlement-gated: the split is only requested for paid / trial users.
//
// The base uses a `distribution` flavor dimension (play/foss), so this module
// must declare the same dimension/flavors — otherwise `implementation(project(":app"))`
// is ambiguous. The `foss` flavor never consumes a split (standalone FOSS APKs
// have no Play split channel); foss bundles this module's sources directly via a
// java.srcDir in app/build.gradle.kts, with ARCore added as fossImplementation.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android {
    namespace = "com.hereliesaz.cuedetat.feature.expert.ar"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") { dimension = "distribution" }
        create("foss") { dimension = "distribution" }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":app"))

    // ARCore now lives here — removed from the base `play` flavor.
    implementation(libs.core)

    // Compose: the module ships TableScanScreen / ArCoreBackground composables.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.material3)

    // CameraX (TableScanAnalyzer) + OpenCV (felt / edge geometry).
    implementation(libs.bundles.camera)
    implementation(libs.opencv)

    implementation(libs.kotlinx.coroutines.android)

    // Hilt runtime only (EntryPointAccessors) — no Hilt codegen in this module.
    implementation(libs.hilt.android)
}
