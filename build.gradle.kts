// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("com.google.dagger.hilt.android") version "2.57" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
    id("com.github.triplet.play") version "3.12.1" apply false// Add this line
    id("com.android.application") version "9.0.0-alpha01" apply false
}