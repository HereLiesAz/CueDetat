pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }

        // Meta Wearables DAT. The single Android variant includes the real
        // integration, so builds that resolve these artifacts need package access.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            content { includeGroup("com.meta.wearable") }

            val localProps = java.util.Properties().apply {
                val file = settingsDir.resolve("local.properties")
                if (file.exists()) file.inputStream().use { load(it) }
            }
            // Credential lookup order matters, and the env-var names are not
            // interchangeable. GitHub Actions sets GITHUB_ACTOR automatically but
            // never exports GITHUB_TOKEN as an env var, and a workflow's built-in
            // GITHUB_TOKEN cannot read another org's packages anyway. Every
            // workflow in .github/workflows therefore passes a PAT as GH_TOKEN
            // with GH_ACTOR alongside it -- those are the names that must be read
            // here. Dropping them yields a blank password and a 401 from
            // maven.pkg.github.com that reads like a permissions problem.
            val ghUser = providers.gradleProperty("gh_user")
                .orElse(providers.environmentVariable("GH_ACTOR"))
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orNull ?: localProps.getProperty("gh_user")
            val ghToken = providers.gradleProperty("gh_token")
                .orElse(providers.environmentVariable("GH_TOKEN"))
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orNull ?: localProps.getProperty("gh_token")

            if (ghUser.isNullOrBlank() || ghToken.isNullOrBlank()) {
                logger.warn(
                    "GitHubPackages credentials are missing or blank. Set gh_user and " +
                        "gh_token in local.properties, or export GH_ACTOR and GH_TOKEN. " +
                        "Android builds that include Meta Wearables cannot resolve them without package access."
                )
            }

            credentials {
                username = ghUser ?: ""
                password = ghToken ?: ""
            }
        }
    }
}

rootProject.name = "CueDetat"

// Fast core-only configuration:
//
//   ./gradlew -Pcuedetat.coreOnly=true allTests
//
// Skips every Android module so the build configures and runs with no Android
// SDK installed. That is the architectural point: the geometry, physics and aim
// maths the product exists to compute are verifiable on any machine in seconds,
// with no emulator. CI uses this for the fast feedback job.
val coreOnly = providers.gradleProperty("cuedetat.coreOnly").orNull?.toBoolean() == true

// ─────────────────────────────────────────────────────────────────────────────
// Kotlin Multiplatform core.
//
// Everything below is pure Kotlin with NO Android, OpenCV or ARCore types. It
// holds the entire model of the game — units, table geometry, ball physics, the
// aim solver, the projection — in SI units, and it is the single source of
// truth the Android app, the Wear app and any future iOS/desktop client render.
//
// These modules run on `jvmTest` with no Android SDK installed, which is the
// point: the geometry that the whole product exists to compute is now testable
// on any machine, in milliseconds, without an emulator.
// ─────────────────────────────────────────────────────────────────────────────
include(":core:units")
include(":core:geometry")
include(":core:physics")
include(":core:projection")
include(":core:aim")
include(":core:advisor")
include(":core:state")

// ─────────────────────────────────────────────────────────────────────────────
// Android delivery.
// ─────────────────────────────────────────────────────────────────────────────
if (!coreOnly) {
    include(":app")

    // The model asset is bundled directly by :app. Expert AR remains in the
    // repository as dormant source but is not part of the build while disabled.

    include(":wear")
}
