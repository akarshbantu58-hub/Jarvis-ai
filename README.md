# Jarvis AI Android App

This repository has been scaffolded as a minimal Android application so it can be built into an APK.

## APK download

Direct APK download (after publishing a release asset named `app-debug.apk`):

https://github.com/akarshbantu58-hub/Jarvis-ai/releases/latest/download/app-debug.apk

If there is no release yet, use the GitHub Actions artifact instead:

- Go to the repository's Actions tab
- Open the latest "Build APK" workflow run
- Download the `debug-apk` artifact
- Extract it and install the APK on your Android device

You can also build the APK locally using the instructions below.

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
