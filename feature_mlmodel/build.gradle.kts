// On-demand dynamic feature module: carries the ~24 MB TFLite "master" model
// (ml/MASTER_POOL_MODEL.tflite) out of the base install for the `play` AAB.
//
// The module is asset-only — it ships no Kotlin/Java. The base app requests it
// at runtime through Play Feature Delivery (see ModelModuleManager) and, once
// installed, MergedTFLiteDetector mmaps the model from the installed split.
//
// Flavors (play/foss) are inherited automatically from the :app module; do not
// redeclare them here. The `foss` flavor never consumes this split — it bundles
// the same asset directly via an assets.srcDir in app/build.gradle.kts, because
// standalone FOSS APKs have no Play split-install channel.
plugins {
    alias(libs.plugins.android.dynamic.feature)
}

android {
    namespace = "com.hereliesaz.cuedetat.feature.mlmodel"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources {
        // The detector mmaps the model via FileChannel.map(), which requires the
        // asset to be stored uncompressed (and page-aligned) inside the split.
        noCompress += "tflite"
    }
}

dependencies {
    implementation(project(":app"))
}
