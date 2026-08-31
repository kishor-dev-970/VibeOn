# VibeOn Session Context — CI Release Build (Sep 1, 2026)

Goal: get GitHub Actions to produce a release APK with no local SDK build, so the
user can run it on Appetize.io.

## Outcome
Run #22 GOING GREEN (first successful CI build). Artifact `VibeOn-v1.5.4-APK`
(~38.8 MB) = `mobile/android/app/build/outputs/apk/release/VibeOn_v1.5.4.apk`.

Trigger: push to `main` (workflow `build.yml`) or tag `v*` (workflow `release.yml`).

## Commits / fixes in this session
Progression of root causes, each fixed one after another:

1. **Autolinking empty `project`** (commits `d68bfa1`)
   - Symptom: `:app:generateAutolinkingPackageList` FAILED with
     `RNGP - Autolinking: Could not find project.android.packageName in react-native config output!`
   - Cause: Expo SDK 57's `expoAutolinking.rnConfigCommand`
     (`node --eval "require('expo/bin/autolinking')" expo-modules-autolinking react-native-config --platform android --json`)
     was writing `autolinking.json` with `"project": {}` in CI (but NOT locally — only
     reproducible in the CI settings phase).
   - Fix: set env `EXPO_USE_COMMUNITY_AUTOLINKING: '1'` on the gradle build step in BOTH
     workflows. This switches `settings.gradle` to the standard community autolinking
     (`autolinkLibrariesFromCommand()`), which produces the correct packageName.
     Verified locally: with the env var the gradle settings phase yields
     `autolinking.json` with `project.android.packageName = com.example.socialmusic`.
   - Also removed temporary diagnostic steps (Dump RN CLI config, Run gradle settings
     phase, Dump generated autolinking.json) from `build.yml`.

2. **Missing native source** (commit `0c1a1d5`)
   - Symptom: `:app:processReleaseMainManifest` — `AndroidManifest.xml` does not exist.
   - Cause: `mobile/.gitignore` had `/android` (Expo managed-workflow default), so the
     whole native folder (incl. AndroidManifest.xml and all custom Kotlin modules) was
     absent from the repo/checkout.
   - Fix: removed `/android` from `mobile/.gitignore` and committed the native source
     (46 files: manifests, res assets, `LocalAudioModule.kt`, `MainActivity.kt`,
     `PlaybackService.kt`, Bravelite modules, etc.). `mobile/android/.gitignore`
     already excludes `build/`, `.gradle/`, `local.properties`, `.cxx/`.

3. **Missing debug keystore** (commit `5dcd9fc`)
   - Symptom: `:app:validateSigningRelease` — `Keystore file .../app/debug.keystore not found for signing config 'debug'`.
   - Cause: `app/build.gradle` signs `release` with `signingConfigs.debug`, whose
     `storeFile file('debug.keystore')` (`mobile/android/app/debug.keystore`) is a
     generated, uncommitted file.
   - Fix: added a `keytool -genkeypair` step in BOTH workflows (alias
     `androiddebugkey`, passwords `android`, RSA 2048) before the gradle build.

## Key local environment facts (for re-running locally)
- JAVA_HOME=`D:\jdk-17-temurin\jdk-17.0.20.1+1`; ANDROID_HOME=ANDROID_SDK_ROOT=`D:\Android`
- Local build (from `mobile/android`, quote the -P arg in PowerShell):
  `.\gradlew.bat :app:assembleRelease "-PreactNativeArchitectures=arm64-v8a,x86_64" --console=plain --no-daemon`
- Release APK named `VibeOn_v{versionName}.apk`; current version 1.5.4 (set in `app/build.gradle`).

## Appetize.io
- Test build app key: `b_iugzqmssvjwzk2o73tvq3odb74`
  (URL `https://appetize.io/app/b_iugzqmssvjwzk2o73tvq3odb74?device=pixel7&osVersion=14.0&scale=75`)
- Token `tok_vlgptys7oclnrnsheaffygojxm` returns "Invalid API token" — upload must be done
  MANUALLY via the Appetize dashboard (download the CI artifact, upload in dashboard).
- The interesting/first working APK is the locally-built `VibeOn_v1.5.4.apk`.

## Next step
Download the `VibeOn-v1.5.4-APK` CI artifact and manually upload to Appetize.
Then iterate on the app as needed.
