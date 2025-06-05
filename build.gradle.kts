// PoolProtractor/build.gradle.kts
plugins {
    id("com.android.application") version "8.12.0-alpha03" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    // Do NOT add an 'android { ... }' block here.
    // This file is for global plugins and configuration, not Android-specific settings.
}

// These blocks are typically at the root level for all subprojects
// and should only contain repository declarations for dependencies
// and plugins that are NOT defined in settings.gradle.kts's pluginManagement.
// For modern Gradle, these are often empty or minimal if settings.gradle.kts is comprehensive.
/*allprojects {
    repositories {
        google()
        mavenCentral()
        // other repositories...
    }
}*/

// Correct way to define a clean task in Kotlin DSL at the root level
tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory) // Use layout.buildDirectory
}