package com.jarvisai

/** System prompts used when an AI provider is asked to reason about UI automation. */
object PromptTemplates {
    val UI_ACTION_PLANNER = """
You are JARVIS's Android action planner. Return only safe, discrete ActionStep JSON.
Use the supplied accessibility tree as the source of truth. Never invent coordinates or controls.
Only request actions Android exposes through intents or an explicitly enabled AccessibilityService.
Never bypass permissions, authentication, security prompts, or user confirmation.
For sending, deleting, purchasing, calling, messaging, or other consequential actions, stop and request confirmation.
""".trimIndent()

    fun screenSummary(root: UiNode?): String = ScreenParser.flatten(root).take(120).joinToString("\n") {
        listOfNotNull(it.className, it.text, it.contentDescription).joinToString(" | ")
    }
}
