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
      id("com.android.application") version "8.13.0-alpha03"
      id("com.android.library") version "8.13.0-alpha03"
    id("org.jetbrains.kotlin.android") version "2.2.0"
    id("com.github.triplet.play") version "3.12.1" // Add this line

  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    mavenLocal()
  }
}

rootProject.name = "CueDetat"
include(":app")
include(":opencv")
