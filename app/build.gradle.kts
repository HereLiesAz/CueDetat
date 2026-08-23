import java.util.Properties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val versionPropsFile = rootProject.file("version.properties")
val versionPropsPath = versionPropsFile.absolutePath
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}

var majorVal = (versionProps.getProperty("MAJOR") ?: "0").toInt()
var minorVal = (versionProps.getProperty("MINOR") ?: "0").toInt()
var patchVal = (versionProps.getProperty("PATCH") ?: "0").toInt()
var buildVal = (versionProps.getProperty("BUILD") ?: "0").toInt()
val lastMajorVal = (versionProps.getProperty("LAST_MAJOR") ?: majorVal.toString()).toInt()
val lastMinorVal = (versionProps.getProperty("LAST_MINOR") ?: minorVal.toString()).toInt()

val isBuildingTask = gradle.startParameter.taskNames.any {
    it.contains("assemble") || it.contains("bundle") || it.contains("install")
}

// CI override: when -PversionBuild=<n> is passed (e.g. the git commit count via
// `git rev-list --count HEAD`), it becomes the authoritative BUILD/versionCode.
// This guarantees a strictly-increasing versionCode for Play uploads without
// relying on the committed version.properties value. When the property is absent
// the original local auto-increment behaviour is preserved untouched.
val versionBuildOverride = project.findProperty("versionBuild")?.toString()?.trim()?.toIntOrNull()

if (versionBuildOverride != null) {
    buildVal = versionBuildOverride
    if (majorVal != lastMajorVal || minorVal != lastMinorVal) {
        patchVal = 0
    }
} else if (isBuildingTask) {
    buildVal++
    if (majorVal != lastMajorVal || minorVal != lastMinorVal) {
        patchVal = 0
    } else {
        patchVal++
    }
}

// Optional explicit versionName override (e.g. -PversionName=1.10.2.999); when
// absent the name is derived from the components below.
val versionNameOverride = project.findProperty("versionName")?.toString()?.trim()?.takeIf { it.isNotEmpty() }

val finalBuild = buildVal
val finalPatch = patchVal
val finalMajor = majorVal
val finalMinor = minorVal
val finalVersionName = versionNameOverride ?: "$finalMajor.$finalMinor.$finalPatch.$finalBuild"
// Only write the computed values back into version.properties for ordinary local
// builds. When CI supplies -PversionBuild we must NOT persist its commit-count
// number into the tracked file (it would clobber the local sequence on the next
// commit). The override is ephemeral and lives only for that build.
val finalIsBuilding = isBuildingTask && versionBuildOverride == null

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val githubAccessToken = localProps.getProperty("GH_TOKEN") ?: ""

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

plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Removed for AGP 9.0 built-in Kotlin
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android {
    namespace = "com.hereliesaz.cuedetat"
    compileSdk = 37

    // The ~24 MB TFLite master model lives in this on-demand dynamic feature
    // module instead of the base install. For the `play` AAB it is delivered
    // via Play Feature Delivery; the `foss` flavor pulls the same asset directly
    // (see the sourceSets block below) because standalone FOSS APKs cannot use
    // split installs.
    dynamicFeatures += setOf(":feature_mlmodel", ":feature_expert_ar")

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 29
        targetSdk = 37
        
        versionCode = finalBuild
        versionName = finalVersionName
        
        buildConfigField("String", "GH_TOKEN", "\"$githubAccessToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 16KB-page Android is 64-bit only. The 32-bit ABIs (armeabi-v7a,
        // x86) ship 4KB-aligned prebuilt .so files and would fail the Play
        // Console 16KB-page compatibility check. x86_64 is also dropped
        // because TFLite 2.17.0's libtensorflowlite_jni.so is 4KB-aligned on
        // x86_64; the next TFLite drop is the LiteRT rebrand and that
        // migration is out of scope here. Real devices that support 16KB
        // pages are arm64-v8a; Play has required 64-bit support since 2019.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            // applicationId stays as "com.hereliesaz.cuedetat" so existing
            // Play closed-testing installs receive an upgrade rather than a
            // side-by-side install.
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
            versionNameSuffix = "-foss"
        }
    }

    // FOSS APKs are distributed standalone (GitHub Releases) and have no Play
    // split-install channel, so they must bundle the TFLite master model
    // directly. Point the foss asset source set at the dynamic feature module's
    // assets so the same physical file is reused with no duplication in git.
    // The `play` flavor deliberately omits this srcDir — its base ships without
    // the model and fetches it on demand from the :feature_mlmodel split.
    sourceSets {
        getByName("foss") {
            assets.srcDir(rootProject.file("feature_mlmodel/src/main/assets"))
            // FOSS APKs have no Play split channel, so the Expert-AR module's
            // sources are compiled directly into the foss APK (ARCore is added as
            // a fossImplementation dependency below). The play flavor omits this
            // and fetches the :feature_expert_ar split on demand instead.
            java.srcDir(rootProject.file("feature_expert_ar/src/main/java"))
        }
    }


    signingConfigs {
        create("release") {
            val ksPath = providers.gradleProperty("KEYSTORE_PATH").orNull ?: System.getenv("KEYSTORE_PATH")
            val ksPassword = providers.gradleProperty("KEYSTORE_PASSWORD").orNull ?: System.getenv("KEYSTORE_PASSWORD")
            val ksAlias = providers.gradleProperty("KEY_ALIAS").orNull ?: System.getenv("KEY_ALIAS")
            val ksKeyPassword = providers.gradleProperty("KEY_PASSWORD").orNull ?: System.getenv("KEY_PASSWORD")

            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Local Myriad backend (emulator loopback) — paired with the cleartext exception
            // in src/debug/res/xml/network_security_config.xml.
            buildConfigField("String", "MYRIAD_BASE_URL", "\"http://10.0.2.2:8000/\"")
        }
        release {
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Empty URL signals AppModule to skip wiring the Myriad client. Override at
            // CI time via Gradle property -PmyriadBaseUrl=https://… once a real backend
            // exists.
            val myriadUrl = providers.gradleProperty("myriadBaseUrl").orNull ?: ""
            buildConfigField("String", "MYRIAD_BASE_URL", "\"$myriadUrl\"")
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
    androidResources {
        noCompress += "tflite"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += "**/libc++_shared.so"
        }
    }
    ndkVersion = "29.0.14206865"
}


// Guard: FOSS is distributed as a standalone APK (assembleFossRelease) only.
// Building a *fused* FOSS artifact (bundleFossRelease or a foss universal APK)
// would package the :feature_expert_ar classes twice — once from the java.srcDir
// compiled into the base (see the foss sourceSet above), once from the fused
// on-demand split (dist:fusing include="true") — producing duplicate classes.
// The assemble path doesn't package dynamic-feature code, so it's safe; abort the
// bundle path with an explanation instead of emitting a broken artifact.
//
// Keyed on the *resolved* task name (not the requested arg) so it can't be
// bypassed by Gradle's camelCase abbreviations (e.g. `bFR`) or by transitive
// inclusion. configureEach stays lazy/configuration-cache friendly: the doFirst
// is only attached if the AAB task is actually realized into the graph.
//
// Match ONLY the per-variant AAB lifecycle tasks (bundleFossDebug /
// bundleFossRelease). A broad `bundleFoss.*` is wrong: it also matches internal
// AGP tasks like `bundleFossDebugClassesToCompileJar` that run during a normal
// assembleFoss* build, which would block the standalone-APK path we rely on.
tasks.configureEach {
    if (name == "bundleFossDebug" || name == "bundleFossRelease") {
        doFirst {
            throw GradleException(
                "Refusing to build a FOSS App Bundle ($name). FOSS ships as a standalone APK — " +
                    "use assembleFossRelease. A fused FOSS bundle would duplicate the " +
                    ":feature_expert_ar classes (java.srcDir + dist:fusing). See docs/RELEASE.md §4.4."
            )
        }
    }
}

// ---------------------------------------------------------------------------
// :wear companion module wiring.
//
// The legacy `dependencies { wearApp project(":wear") }` DSL (which embedded a
// Wear 1.x "unbundled" APK inside the phone APK's resources for automatic push
// install) no longer exists in this project's AGP version — the `wearApp`
// configuration is not registered by AGP 9.2.1 (confirmed: declaring it fails
// with "Configuration with name 'wearApp' not found", and no such configuration
// name appears anywhere in the AGP 9.2.1 jar). This tracks upstream AGP/Wear OS
// guidance: standalone Wear OS 3+ apps like :wear (minSdk 30, its own
// applicationId) are no longer packaged *inside* the phone APK/AAB by Gradle at
// all. Instead they're uploaded as a separate artifact under the *same* Play
// Console app listing (Play Console → App bundle explorer → Wear release
// track), and Play associates/pushes the watch app to the paired device based
// on the shared package name + signing key — see
// https://developer.android.com/training/wearables/apps/creating#embed-app-in-phone-app-module
// for the pattern this superseded.
//
// Because there's no Gradle dependency to declare, it's easy for the watch
// module to silently bit-rot unbuilt and unnoticed. To prevent that, make sure
// :wear is at least compiled/assembled every time this app is assembled or
// bundled, so a broken watch app fails the phone app's build too instead of
// going unnoticed until a manual Play upload.
tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("bundle") }.configureEach {
    dependsOn(":wear:assembleDebug")
}


dependencies {
    // Bouncy Castle is pulled in transitively. The resolutionStrategy below
    // (configurations.all) force-upgrades it at resolution time, but Dependabot
    // parses build files statically and does not evaluate that dynamic override,
    // so it keeps flagging the vulnerable transitive versions (CVE-2026-5598 and
    // the older bcpkix/LDAP-injection advisories). These explicit constraints
    // pin the patched 1.84 in a form static analysis recognizes, so the alerts
    // resolve. Keep the version in sync with libs.versions.bouncycastle.
    constraints {
        val bcVersion = libs.versions.bouncycastle.get()
        implementation("org.bouncycastle:bcprov-jdk18on:$bcVersion")
        implementation("org.bouncycastle:bcpkix-jdk18on:$bcVersion")
        implementation("org.bouncycastle:bcutil-jdk18on:$bcVersion")
        implementation("org.bouncycastle:bctls-jdk18on:$bcVersion")
    }

    implementation(libs.androidx.datastore.preferences)

    // Play Feature Delivery — drives on-demand SplitInstall of the
    // :feature_mlmodel dynamic feature. Play-flavor only: the foss flavor
    // bundles the model directly and never performs split installs.
    "playImplementation"(libs.play.feature.delivery)

    // The metric core. Pure Kotlin Multiplatform: units, table geometry,
    // ball physics, the aim solver and the projection. No Android types cross
    // this boundary in either direction.
    implementation(project(":core:units"))
    implementation(project(":core:geometry"))
    implementation(project(":core:physics"))
    implementation(project(":core:projection"))
    implementation(project(":core:aim"))
    implementation(project(":core:advisor"))
    implementation(project(":core:state"))

    // Core & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    // Compose
        implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

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
    implementation(libs.retrofit.converter.gson)
    implementation(libs.aznavrail)

    // Location
    implementation(libs.play.services.location)

    // Computer Vision
    implementation(libs.mlkit.detection)
    implementation(libs.opencv)

    // ARCore now lives in the on-demand :feature_expert_ar dynamic feature, so
    // the play base AAB ships without it. The foss flavor compiles that module's
    // sources directly into the APK (see the foss sourceSet above), so it needs
    // ARCore on its own classpath.
    "fossImplementation"(libs.arcore)

    // TFLite — pocket detection model
    implementation(libs.tensorflow.lite)
    // GPU delegate — offloads FP16 inference to the GPU when the device supports it,
    // with a CPU/NNAPI fallback handled in MergedTFLiteDetector.
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.api)

    // Meta Wearables DAT. Served only from a credentialed GitHub Packages
    // registry, so it is scoped to the play flavor: previously :app depended on
    // these unconditionally and `./gradlew assembleFossDebug` from a clean clone
    // failed at dependency resolution for every outside contributor -- the exact
    // audience a FOSS flavor exists for.
    "playImplementation"(libs.mwdat.core)
    "playImplementation"(libs.mwdat.camera)
    // Note: flavour+buildType configurations ("playDebugImplementation") are not
    // available at this point in configuration, so the mock device rides on the
    // flavour configuration instead. It is debug tooling and unreferenced in
    // release, so R8 strips it; the property that matters is preserved -- the
    // foss flavour never needs the credentialed registry.
    "playImplementation"(libs.mwdat.mockdevice)

    // Wear OS Data Layer
    implementation(libs.play.services.wearable)


    // Physics
    // implementation(libs.google.liquidfun)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
        debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // implementation("cljsjs:liquidfun:1.1.0-0")

    constraints {
        implementation(libs.guava) {
            because("Transitive dependency vulnerability")
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
            useVersion(libs.versions.netty.get())
            because("Transitive dependency vulnerabilities in testing/grpc")
        }
        if (requested.group == "org.bouncycastle") {
            useVersion(libs.versions.bouncycastle.get())
            because("Force-upgrade Bouncy Castle modules to fix vulnerabilities and ensure version alignment")
        }
    }
}
