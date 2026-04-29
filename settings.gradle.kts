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
            credentials {
                username = ""
                password = providers.gradleProperty("github_token")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .getOrElse("")
            }
        }
    }
}

rootProject.name = "CueDetat"
include(":app")
