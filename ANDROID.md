# JARVIS Ultimate for Android

The Android wrapper loads the published JARVIS workspace securely, so AI, voice, saved chats, generated images, and account features stay connected to the same backend as the website.

## Build a debug APK

Requirements: Android Studio (current stable release) with its bundled JDK and Android SDK.

```bash
bun install
bun run android:sync
bun run android:open
```

In Android Studio, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The resulting debug APK is written under `android/app/build/outputs/apk/debug/`.

## Build a signed release

In Android Studio, select **Build > Generate Signed App Bundle or APK**, choose **APK**, and create or select a private signing key. Never commit the signing key or its passwords.

## Refresh native settings

After changing `capacitor.config.ts` or Android plugins, run:

```bash
bun run android:sync
```

The website itself is hosted, so ordinary JARVIS interface updates do not require rebuilding the APK.