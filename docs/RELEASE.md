# Releasing Cue D’état

Cue D’état ships one Android application variant with one package identity:
`com.hereliesaz.cuedetat`. There is no `play`/`foss` product-flavor split.

## Build artifacts

```bash
# Google Play
./gradlew bundleRelease -PversionBuild=$(git rev-list --count HEAD)

# GitHub Releases / direct install
./gradlew assembleRelease -PversionBuild=$(git rev-list --count HEAD)
```

Both artifacts come from the same `main` source set and dependency graph. The
TFLite master model remains physically stored under
`feature_mlmodel/src/main/assets/ml/`, but `:app` includes that directory as a
normal main asset source. It is not delivered as a dynamic feature.

Expert AR/table scan is intentionally disabled while its native ARCore path is
being repaired and verified. The `:feature_expert_ar` source remains in the
repository, but it is not included by `settings.gradle.kts`, ARCore is not on
the app runtime classpath, the UI exposes no AR/table-scan controls, and the base
state/event boundary rejects attempts to enter `AR_SETUP` or `AR_ACTIVE`.

## Signing

`app/build.gradle.kts` reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` from Gradle properties or environment
variables. Without a release keystore, local release builds fall back to the
debug key; published releases must supply the real signing credentials.

CI may inject signing directly:

```bash
./gradlew bundleRelease -PversionBuild=$(git rev-list --count HEAD) \
  -Pandroid.injected.signing.store.file=$PWD/app/keystore.jks \
  -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
  -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
  -Pandroid.injected.signing.key.password=$KEY_PASSWORD
```

Never commit keystores, package tokens, or `local.properties`.

## Versioning

`-PversionBuild=<n>` is the authoritative CI `versionCode` and final
`versionName` segment. CI normally uses `git rev-list --count HEAD`, keeping
Play uploads monotonic without writing the CI number back into
`version.properties`.

Without the override, local assemble/bundle/install tasks retain the existing
automatic increment behavior. `-PversionName=<string>` remains available as an
explicit name override.

## Dependency credentials

The sole variant includes the real Meta Wearables DAT integration. Its artifacts
come from GitHub Packages and require `GH_ACTOR` plus `GH_TOKEN` (or
`gh_user` / `gh_token` Gradle/local properties) during dependency resolution.

Other release secrets include the signing secrets above and
`PLAY_SERVICE_ACCOUNT_JSON` for Play publishing.

## Canonical CI task names

- tests: `testDebugUnitTest`
- debug APK: `assembleDebug` → `app/build/outputs/apk/debug/`
- release APK: `assembleRelease` → `app/build/outputs/apk/release/`
- release AAB: `bundleRelease` → `app/build/outputs/bundle/release/`

Centralized workflow definitions must use these ordinary variant tasks as well.

## Play notes

The AAB still lets Google Play generate device-specific splits automatically.
The app currently restricts native packaging to `arm64-v8a` for its 16 KB page
compatibility policy. When Play App Signing is enabled, the CI keystore is the
upload key.
