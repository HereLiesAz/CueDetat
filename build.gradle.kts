// Top-level build file where you can add configuration options common to all subprojects/modules.
buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.jdom:jdom2:2.0.6.1")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

// Force-upgrade transitive Netty (pulled via grpc/testing) to a patched version across EVERY module,
// so the dependency graph submitted to GitHub (the `submit-gradle` job) is clean and the Dependabot
// Netty advisories resolve. The :app module also forces io.netty in its own block; this additionally
// covers the dynamic-feature modules.
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty") {
                useVersion(libs.versions.netty.get())
                because("Transitive Netty CVEs (HTTP/2 flood/MadeYouReset, SNI 16MiB alloc, smuggling, etc.)")
            }
        }
    }
}
