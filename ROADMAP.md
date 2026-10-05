# JARVIS OS Production Roadmap

## Stage 0 — Build foundation

- Native Android/Kotlin project identified and preserved
- Gradle settings repaired
- GitHub Actions Android APK pipeline repaired
- JDK 17 / Android SDK 36 / Gradle 8.7 build environment
- Debug and unsigned release APK artifacts

## Stage 1 — Core architecture

- AI provider interface and provider manager
- Command model + SAFE/SENSITIVE/DANGEROUS confirmation policy
- Secure settings/secrets abstraction
- Conversation memory abstraction
- Unified permission/special-access status model

## Stage 2 — Voice

- Robust speech-recognition lifecycle
- Interruptible TTS
- continuous-conversation mode
- push-to-talk fallback
- wake-word adapter with offline-first engine where device support permits
- battery-saving modes

## Stage 3 — Android assistant integration

- Default assistant lifecycle
- assistant session UI
- overlay flow
- supported assist context handling

## Stage 4 — Authorized automation

- AccessibilityService with explicit user activation
- safe app launching
- supported UI actions
- confirmation gates for sensitive actions
- audit log

## Stage 5 — Vision and screen context

- CameraX-based preview/analysis
- explicit MediaProjection screen capture
- AI vision provider abstraction
- visible capture indicators

## Stage 6 — Notifications and device information

- NotificationListenerService with explicit access
- battery/network/device information
- supported device controls

## Stage 7 — Premium UI

- Compose introduced incrementally
- responsive tablet layouts
- reduced-motion/accessibility options
- Liquid-glass-inspired original visual system
- state-driven JARVIS core animations

## Stage 8 — Production hardening

- instrumentation tests
- offline behavior tests
- permission denial tests
- API failure tests
- release signing through GitHub Secrets
- Play Store policy review
