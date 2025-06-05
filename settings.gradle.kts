// PoolProtractor/settings.gradle.kts
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
  }
  plugins {
    id("com.android.application") version "8.12.0-alpha03"
    id("com.android.library") version "8.12.0-alpha03"
    id("org.jetbrains.kotlin.android") version "2.1.21"
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "CueDetat"

include(":app")
