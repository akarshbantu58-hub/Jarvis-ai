# JARVIS OS

JARVIS OS is a native Android/Kotlin assistant application designed to run on Android devices and tablets.

## Current project type

- **Technology:** Native Android application
- **Language:** Kotlin
- **Build system:** Gradle + Android Gradle Plugin
- **Application ID:** `com.jarvisai`
- **Compile / target SDK:** Android 16 / API 36
- **Minimum SDK:** 24
- **Java:** 17

This repository is **not** a Python/Kivy or Flutter project, so Buildozer/python-for-android and Flutter build steps are not used.

## Build APK without a PC

GitHub Actions builds the APK for you.

### Beginner steps

1. Open GitHub.
2. Open the **Jarvis-ai** repository.
3. Open **Actions**.
4. Select **JARVIS OS - Android APK**.
5. Tap **Run workflow**.
6. Wait for the workflow to finish.
7. Open the completed workflow run.
8. Scroll to **Artifacts**.
9. Download **JARVIS-OS-Android-APKs**.
10. Extract the downloaded artifact and install `JARVIS-OS-debug.apk` on the Galaxy Tab A9.

The workflow also produces an unsigned release APK when the Android release task succeeds. An unsigned release APK is useful for build verification but is not the final Play Store artifact.

## What the workflow validates

The workflow:

1. Checks out the repository.
2. Installs JDK 17.
3. Installs Android SDK 36 and Build Tools 36.0.0.
4. Installs Gradle 8.7.
5. Runs Android lint.
6. Runs debug unit tests.
7. Builds the debug APK.
8. Builds the release variant.
9. Renames the APKs to predictable `JARVIS-OS-*` names.
10. Uploads them as GitHub Actions artifacts.

## If the workflow fails

Open the failed workflow run and expand the step with the red X.

- **Lint failure:** read the first `error:` entry and fix that source/resource issue.
- **Kotlin/Java compilation failure:** read the first compiler error; later errors may be consequences of the first one.
- **SDK failure:** the workflow installs API 36 automatically; rerun once before changing project files.
- **Gradle failure:** inspect the Gradle error near the bottom of the log. Do not randomly change dependency versions.
- **Artifact failure:** verify that the build step succeeded and that `app/build/outputs/apk/debug/app-debug.apk` was produced.

## API keys and secrets

Do not commit Gemini, OpenAI-compatible, ElevenLabs, or other private API keys into the repository.

JARVIS stores user API configuration locally through its API Hub. If a future GitHub Actions step needs a secret, use **GitHub repository Secrets** instead of hard-coding it in source or workflow files.

## Release signing

The current Actions pipeline intentionally builds an **unsigned release APK** and an installable **debug APK**. A Play Store release requires your own signing key/keystore.

Never commit the keystore or its passwords to GitHub. For a future signed-release workflow, store the keystore and passwords in GitHub Secrets and reconstruct the keystore only inside the Actions runner.

## Android capability limitations

JARVIS requests Android permissions through Android's normal permission system. Capabilities such as default-assistant integration, overlay windows, AccessibilityService, MediaProjection screen capture, microphone access, camera access, and foreground services are subject to Android and device/OEM user approval. The application cannot silently bypass those protections.
