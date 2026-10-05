# JARVIS OS Architecture

## Current implementation

JARVIS OS is a native Android/Kotlin application. The existing XML/View UI and working assistant components are intentionally preserved while the project is hardened in stages.

### Layers

- `MainActivity` — presentation/orchestration entry point
- `JarvisCore` — speech recognition intent + Android Text-to-Speech lifecycle
- `ApiHub` / `ApiClient` — configurable AI provider configuration and HTTP client
- `AppActionEngine` — safe, explicit Android app-launch actions
- `PermissionCenter` / `PermissionActivity` — runtime/special-access setup
- `JarvisVoiceInteractionService` — Android default-assistant entry point
- `JarvisSessionService` — assistant session implementation
- `JarvisOverlayService` — optional user-enabled overlay
- `Automation*` — scheduled user-defined automation
- `VoiceOrbView` — animated assistant state visualization

## Migration strategy

The repository does not currently use Jetpack Compose. A full rewrite would unnecessarily risk the existing assistant-role and voice functionality, so the first stage keeps the proven XML/View UI. Compose will be introduced incrementally for new screens and then used for the redesigned home/settings surfaces once CI is green.

## Feature boundaries

Each future capability must be isolated behind an interface/service boundary:

- AI providers
- Speech recognition / wake word
- Text-to-speech
- Command execution
- Accessibility automation
- Camera / vision
- MediaProjection screen analysis
- Notification access
- Memory
- Secure settings / secrets
- Device controls

Android permissions and special-access flows remain explicit and user-controlled.
