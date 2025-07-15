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
  plugins {
    id("com.android.application") version "8.12.0"
    id("com.android.library") version "8.12.0-alpha09"

    id("org.jetbrains.kotlin.android") version "2.2.0"
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
include(":opencv")
