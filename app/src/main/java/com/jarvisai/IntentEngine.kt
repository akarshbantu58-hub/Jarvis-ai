package com.jarvisai

import java.util.Locale

/** Converts free-form user input into a small, testable command taxonomy. */
class IntentEngine {
    fun classify(command: String): JarvisIntent {
        val raw = command.trim()
        if (raw.isBlank()) return JarvisIntent(raw, JarvisIntent.Type.EMPTY)

        val lower = raw.lowercase(Locale.getDefault())
        val type = when {
            lower.contains("api hub") || lower.contains("api settings") ->
                JarvisIntent.Type.OPEN_API_HUB
            lower.contains("permissions") || lower.contains("permission settings") ->
                JarvisIntent.Type.OPEN_SETTINGS
            lower.contains("automation") || lower.contains("automations") ->
                JarvisIntent.Type.OPEN_AUTOMATION
            lower.contains("default assistant") || lower.contains("make jarvis default") ->
                JarvisIntent.Type.DEFAULT_ASSISTANT
            else -> JarvisIntent.Type.AI_QUERY
        }

        return JarvisIntent(rawCommand = raw, type = type)
    }
}
