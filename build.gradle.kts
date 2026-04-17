// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.jdom:jdom2:2.0.6.1")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
