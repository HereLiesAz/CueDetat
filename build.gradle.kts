// Top-level build file where you can add configuration options common to all subprojects/modules.
buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.jdom:jdom2:2.0.6.1")
            // The Android Gradle Plugin itself (com.android.tools.build:gradle, via
            // sdklib/repository) bundles a vulnerable Bouncy Castle 1.79 in this root
            // buildscript classpath — this loads AGP before `subprojects{}` below is
            // even evaluated, so that force doesn't reach it. Force it here too so the
            // root project's own classpath resolves the patched version as well.
            force("org.bouncycastle:bcprov-jdk18on:1.84", "org.bouncycastle:bcpkix-jdk18on:1.84", "org.bouncycastle:bcutil-jdk18on:1.84")
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

// Force-upgrade transitive Netty (pulled via grpc/testing, e.g. ARCore's io.grpc:grpc-netty
// in :feature_expert_ar) and Bouncy Castle (pulled via AGP's own lint/sdklib tooling, e.g. the
// androidLintTool configuration in every module) to patched versions across EVERY module, so the
// dependency graph submitted to GitHub (the `submit-gradle` job) is clean and the Dependabot
// advisories resolve. The :app module also forces both groups in its own block (plus explicit
// Bouncy Castle constraints for static analysis); this additionally covers the dynamic-feature
// modules (:feature_expert_ar, :feature_mlmodel) and :wear, which don't have their own force and
// were otherwise still resolving vulnerable org.bouncycastle:bcprov-jdk18on/bcpkix-jdk18on:1.79
// via their androidLintTool classpath.
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty") {
                useVersion(libs.versions.netty.get())
                because("Transitive Netty CVEs (HTTP/2 flood/MadeYouReset, SNI 16MiB alloc, smuggling, etc.)")
            }
            if (requested.group == "org.bouncycastle") {
                useVersion(libs.versions.bouncycastle.get())
                because("Transitive Bouncy Castle CVEs (GOST 28147 CTR keystream reuse, LDAP injection, etc.)")
            }
        }
    }
}
