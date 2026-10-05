# JARVIS OS Architecture

JARVIS OS uses a component-based Android architecture. The current app is intentionally kept on the existing Kotlin + Android Views stack so working functionality is preserved while the agent layer is expanded. A future Compose UI can sit above these same interfaces without rewriting the execution layer.

## Agent data flow

`WakeWordEngine -> VoiceAssistantController -> Agent/AppActionEngine -> AgentPlanner -> ActionStep -> GestureController / IntentDispatcher -> ActionStateVerifier -> TextToSpeechManager`

For screen-aware automation:

`JarvisAccessibilityService -> ScreenParser -> UiNode tree -> PromptTemplates -> AgentPlanner -> ActionStep`

## Components

- `UiNode.kt` — clean accessibility data model.
- `ActionStep.kt` — discrete executable command model.
- `ExecutionSession.kt` — current execution state.
- `ScreenParser.kt` — converts AccessibilityNodeInfo into UiNode data.
- `PromptTemplates.kt` — safety-oriented prompts for UI planning.
- `AgentPlanner.kt` — deterministic local planner for common UI actions; cloud AI can be added behind this boundary.
- `GestureController.kt` — executes only through the user-enabled Accessibility Service.
- `ActionStateVerifier.kt` — reports execution success/failure.
- `IntentDispatcher.kt` — system-level app/settings/URL intents.
- `TextToSpeechManager.kt` — replaceable Android TTS boundary.
- `JarvisAccessibilityService.kt` — explicit user-enabled UI automation bridge.
- `JarvisOverlayService.kt` — optional floating JARVIS status UI after overlay permission.

## Security model

JARVIS never enables Accessibility, notification access, overlay access, microphone, camera, or screen capture programmatically. Android Settings and runtime permission dialogs remain the authority. Consequential actions must pass the command-risk/confirmation layer before execution.

## Current reality

The planner can execute deterministic actions such as Home, Back, Scroll, Click text, Type text, and Read screen. App launching and device actions continue through the existing Android intent/action layer. Wake-word detection remains a compatibility architecture using Android speech recognition with offline preference; it is not a claim of unrestricted always-on microphone access.
