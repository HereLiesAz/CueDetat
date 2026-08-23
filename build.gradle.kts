// Top-level build file. Configuration common to every module lives here.

buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.jdom:jdom2:2.0.6.1")
            // The Android Gradle Plugin itself (com.android.tools.build:gradle, via
            // sdklib/repository) bundles a vulnerable Bouncy Castle in this root
            // buildscript classpath — this loads AGP before `subprojects{}` below is
            // even evaluated, so that force doesn't reach it. Force it here too so the
            // root project's own classpath resolves the patched version as well.
            force(
                "org.bouncycastle:bcprov-jdk18on:1.84",
                "org.bouncycastle:bcpkix-jdk18on:1.84",
                "org.bouncycastle:bcutil-jdk18on:1.84",
            )
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

// Force-upgrade transitive Netty (pulled via grpc/testing, e.g. ARCore's io.grpc:grpc-netty
// in :feature_expert_ar) and Bouncy Castle (pulled via AGP's own lint/sdklib tooling) to
// patched versions across EVERY module, so the dependency graph submitted to GitHub is clean
// and the Dependabot advisories resolve.
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty") {
                useVersion(libs.versions.netty.get())
                because("Transitive Netty CVEs (HTTP/2 flood/MadeYouReset, SNI 16MiB alloc, smuggling)")
            }
            if (requested.group == "org.bouncycastle") {
                useVersion(libs.versions.bouncycastle.get())
                because("Transitive Bouncy Castle CVEs (GOST 28147 CTR keystream reuse, LDAP injection)")
            }
        }
    }
}
