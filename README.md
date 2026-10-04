# Jarvis AI Android App

This repository has been scaffolded as a minimal Android application so it can be built into an APK.

## APK download

Download the latest debug APK from the GitHub Actions artifact when the build workflow runs.

- Go to the repository's Actions tab
- Open the latest "Build APK" workflow run
- Download the `debug-apk` artifact
- Extract it and install the APK on your Android device

## Build instructions

Requirements:
- Android Studio or Android SDK
- JDK 17+
- Gradle (or use the Android Gradle plugin via Android Studio)

From the project root, run:

```bash
./gradlew assembleDebug
```

The generated APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Included
- Android manifest
- Main activity in Kotlin
- Material app theme
- Basic UI layout for a Jarvis AI launcher screen

## Notes
The repository was empty, so this is a clean starter app scaffold intended to be built as an APK.
