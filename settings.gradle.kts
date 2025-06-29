pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Add the AndroidX snapshot repository for pre-release libraries
    maven { url = uri("https://androidx.dev/snapshots/builds/LATEST/repository") }
  }
}
rootProject.name = "Cue D'etat"
include(":app")