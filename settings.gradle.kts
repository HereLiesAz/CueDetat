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
    // Required for AndroidX pre-release libraries
    maven { url = uri("https://androidx.dev/snapshots/builds/LATEST/repository") }
  }
}
rootProject.name = "Cue D'etat"
include(":app")
