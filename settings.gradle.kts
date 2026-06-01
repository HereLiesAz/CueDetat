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
            val ghUser = providers.gradleProperty("gh_user")
                .orElse(providers.environmentVariable("GH_ACTOR"))
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orNull
            val ghToken = providers.gradleProperty("gh_token")
                .orElse(providers.environmentVariable("GH_TOKEN"))
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orNull
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
