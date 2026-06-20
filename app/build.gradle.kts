import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class FetchTesterEmailsTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val ggLink: Property<String>

    @get:Input
    @get:Optional
    abstract val ggSession: Property<String>

    @get:InputDirectory
    abstract val scriptDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun fetch() {
        val outFile = outputDir.file("tester_emails.txt").get().asFile
        outFile.parentFile.mkdirs()

        val link = ggLink.orNull
        val session = ggSession.orNull

        if (link.isNullOrBlank() || session.isNullOrBlank()) {
            logger.lifecycle("[testerLicense] GG_LINK or GG_SESSION absent — writing empty allowlist.")
            outFile.writeText("")
            return
        }

        val script = File(scriptDir.get().asFile, "fetch-tester-emails.mjs")
        if (!script.exists()) {
            logger.warn("[testerLicense] scraper script missing at $script — writing empty allowlist.")
            outFile.writeText("")
            return
        }

        val nodeModules = File(scriptDir.get().asFile, "node_modules/playwright")
        if (!nodeModules.exists()) {
            logger.lifecycle("[testerLicense] installing scraper dependencies (npm ci)…")
            execOperations.exec {
                workingDir = scriptDir.get().asFile
                commandLine = listOf("npm", "ci", "--no-audit", "--no-fund")
                isIgnoreExitValue = true
            }
        }

        val stdoutCollector = ByteArrayOutputStream()
        val result = execOperations.exec {
            workingDir = scriptDir.get().asFile
            commandLine = listOf("node", script.absolutePath)
            environment("GG_LINK", link)
            environment("GG_SESSION", session)
            standardOutput = stdoutCollector
            isIgnoreExitValue = true
        }

        val raw = stdoutCollector.toString(Charsets.UTF_8).trim()
        if (result.exitValue != 0) {
            logger.warn("[testerLicense] scraper exited ${result.exitValue}; writing whatever it emitted (${raw.lines().size} lines).")
        }
        outFile.writeText(raw + if (raw.isEmpty()) "" else "\n")
        logger.lifecycle("[testerLicense] wrote ${raw.lines().count { it.isNotBlank() }} hashes to ${outFile.name}.")
    }
}

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
val googleCloudProjectNumber = localProps.getProperty("GOOGLE_CLOUD_PROJECT_NUMBER") ?: "0"
val githubAccessToken = localProps.getProperty("GH_TOKEN") ?: ""
// OAuth 2.0 *Web* client ID from Google Cloud Console. Required by
// Credential Manager's GetGoogleIdOption. Empty when absent: the tester
// auto-resolve falls back to no-op so the build still works for
// contributors who don't have credentials configured.
val googleOauthWebClientId = localProps.getProperty("GOOGLE_OAUTH_WEB_CLIENT_ID") ?: ""

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
    dynamicFeatures += setOf(":feature_mlmodel")

    defaultConfig {
        applicationId = "com.hereliesaz.cuedetat"
        minSdk = 29
        targetSdk = 37
        
        versionCode = finalBuild
        versionName = finalVersionName
        
        buildConfigField("long", "GOOGLE_CLOUD_PROJECT_NUMBER", "${googleCloudProjectNumber}L")
        buildConfigField("String", "GH_TOKEN", "\"$githubAccessToken\"")
        buildConfigField("String", "GOOGLE_OAUTH_WEB_CLIENT_ID", "\"$googleOauthWebClientId\"")

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
        }
    }

    // The play-release build pulls a generated tester-email allowlist asset
    // from a gradle task (see fetchTesterEmails below). The file lives outside
    // src/play/assets/ so it is never tracked in git.

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

    // Play Billing — only included in the play flavor APK.
    "playImplementation"(libs.androidx.billing.ktx)

    // Credential Manager + Google ID — only included in the play flavor APK.
    // Used to read the device's currently-signed-in Google account email
    // (via a one-tap account picker on first use) so the app can match it
    // against the tester-license allowlist baked into the build.
    "playImplementation"(libs.androidx.credentials)
    "playImplementation"(libs.androidx.credentials.play.services.auth)
    "playImplementation"(libs.google.id)

    // Core & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

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
    // Override the older kotlin-metadata-jvm that hilt-compiler bundles so the
    // KSP processor can parse Kotlin 2.4.0 metadata (highest-version wins).
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
    implementation(libs.core)

    // TFLite — pocket detection model
    implementation(libs.tensorflow.lite)
    // GPU delegate — offloads FP16 inference to the GPU when the device supports it,
    // with a CPU/NNAPI fallback handled in MergedTFLiteDetector.
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.api)

    // Meta Wearables DAT
    implementation(libs.mwdat.core)
    implementation(libs.mwdat.camera)
    debugImplementation(libs.mwdat.mockdevice)

    // Security & Integrity
    implementation(libs.play.integrity)

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
    implementation(libs.kotlin.stdlib)

    constraints {
        implementation(libs.jose4j) {
            because("Transitive dependency vulnerability")
        }
        implementation(libs.guava.vulnerability) {
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
        // Hilt/Dagger's annotation processor bundles an older kotlin-metadata-jvm that can't parse
        // metadata emitted by the Kotlin 2.4.0 compiler ("version 2.4.0, max supported 2.3.0"),
        // which fails hiltJavaCompile. Force it to match the Kotlin version on every configuration
        // (incl. the annotation-processor classpath, which a plain implementation() dep doesn't cover).
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
            useVersion(libs.versions.kotlinMetadataJvm.get())
            because("Hilt's processor must support the Kotlin compiler's metadata version")
        }
    }
}

// ---------------------------------------------------------------------------
// Tester license allowlist: scrapes the tester Google Group's member list
// during the play-release pipeline and emits one SHA-256 hex per email into
// build/generated/testerLicense/assets/tester_emails.txt. The file is then
// consumed by TestLicenseAllowlist at runtime.
//
// Skips silently (writes an empty file) when:
//   - GG_LINK or GG_SESSION env vars are missing (local dev / play debug)
//   - The Node script fails for any reason
//
// Hooked into the asset generation pipeline for playRelease so other build
// variants are unaffected, including IDE sync.
// ---------------------------------------------------------------------------
val fetchTesterEmails = tasks.register<FetchTesterEmailsTask>("fetchTesterEmails") {
    description = "Scrape tester Google Group members and emit SHA-256 hashes for the play-release allowlist."
    group = "tester license"
    outputDir.set(layout.buildDirectory.dir("generated/testerLicense/assets"))
    scriptDir.set(rootProject.layout.projectDirectory.dir(".github/scripts"))
    ggLink.set(providers.environmentVariable("GG_LINK"))
    ggSession.set(providers.environmentVariable("GG_SESSION"))
    outputs.upToDateWhen { false } // env-driven; always re-fetch when invoked
}

androidComponents.onVariants { variant ->
    if (variant.flavorName == "play" && variant.buildType == "release") {
        variant.sources.assets?.addGeneratedSourceDirectory(
            fetchTesterEmails,
            FetchTesterEmailsTask::outputDir
        )
    }
}
