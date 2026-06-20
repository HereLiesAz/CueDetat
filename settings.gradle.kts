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
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")

    }
    plugins {
        id("com.android.application") version "9.2.1"
        id("com.android.library") version "9.2.1"
        id("com.android.dynamic-feature") version "9.2.1"
        id("com.github.triplet.play") version "4.0.0"

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = "https://jitpack.io")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            // Only serve Meta Wearable artifacts from this repo. Without this
            // filter Gradle queries GitHub Packages for every dependency, and
            // any unauthenticated 401 (e.g. for TFLite, which lives on Maven
            // Central) aborts the whole build instead of falling through.
            content {
                includeGroup("com.meta.wearable")
            }
            val localProps = java.util.Properties().apply {
                val file = settingsDir.resolve("local.properties")
                if (file.exists()) {
                    file.inputStream().use { load(it) }
                }
            }

            val ghUser = providers.gradleProperty("gh_user")
                .orElse(providers.gradleProperty("GH_USER"))
                .orElse(providers.environmentVariable("GH_ACTOR"))
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orNull ?: localProps.getProperty("gh_user") ?: localProps.getProperty("GH_USER") ?: localProps.getProperty("GH_ACTOR")

            val ghToken = providers.gradleProperty("gh_token")
                .orElse(providers.gradleProperty("GH_TOKEN"))
                .orElse(providers.environmentVariable("GH_TOKEN"))
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orNull ?: localProps.getProperty("gh_token") ?: localProps.getProperty("GH_TOKEN")

            if (ghUser.isNullOrBlank() || ghToken.isNullOrBlank()) {
                logger.warn(
                    "⚠ GitHubPackages credentials missing. Set 'gh_user' and 'gh_token' " +
                    "in local.properties (or GITHUB_ACTOR/GITHUB_TOKEN env vars). Builds that need " +
                    "com.meta.wearable artifacts will fail at resolution."
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
include(":app")
// On-demand dynamic feature module carrying the 24 MB TFLite master model.
// Delivered via Play Feature Delivery for the `play` AAB; the `foss` flavor
// bundles the same asset directly (see app/build.gradle.kts), since standalone
// FOSS APKs have no Play split-install channel.
include(":feature_mlmodel")
