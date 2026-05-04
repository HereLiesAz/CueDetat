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
        id("com.android.application") version "9.2.0"
        id("com.android.library") version "9.2.0"
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
            val ghUser = providers.gradleProperty("github_user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orNull
            val ghToken = providers.gradleProperty("github_token")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orNull
            if (ghUser.isNullOrBlank() || ghToken.isNullOrBlank()) {
                logger.warn(
                    "⚠ GitHubPackages credentials missing. Set 'github_user' and 'github_token' " +
                    "in local.properties (or GITHUB_ACTOR/GITHUB_TOKEN env vars). Builds that need " +
                    "com.facebook.meta-wearables-dat-android artifacts will fail at resolution."
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
