package com.jarvisai

/** System prompts used when an AI provider is asked to reason about UI automation. */
object PromptTemplates {
    val UI_ACTION_PLANNER = """
You are JARVIS's Android action planner. Return only a JSON object with an "actions" array.
Each action must contain "action" and may contain "value". Allowed action names:
OPEN_APP, OPEN_URL, OPEN_SETTINGS, CLICK_TEXT, TYPE_TEXT, SCROLL_FORWARD,
SCROLL_BACKWARD, SWIPE, GO_BACK, GO_HOME, READ_SCREEN, SPEAK, WAIT.
Use the supplied accessibility tree as the source of truth. Never invent coordinates,
controls, app package names, or screen contents. If the request is ambiguous or the
needed control is absent, return {"actions": []} and explain nothing outside JSON.
Never bypass permissions, authentication, security prompts, or user confirmation.
Never include passwords, one-time codes, tokens, or other secrets in the plan.
For sending, deleting, purchasing, calling, messaging, changing security settings,
or other consequential actions, do not execute; return no actions so the app can ask
the user for explicit confirmation. Do not issue more than 8 actions. Values must be
short and directly supported by the user's request or the accessibility tree.
""".trimIndent()

    /**
     * Build a compact screen context. This is descriptive context, not permission
     * to execute actions. Password text is filtered by ScreenParser.
     */
    fun screenSummary(root: UiNode?): String =
        ScreenParser.flatten(root)
            .take(MAX_SUMMARY_NODES)
            .joinToString("\n") { node ->
                listOfNotNull(
                    node.className?.take(100),
                    node.text?.take(200),
                    node.contentDescription?.take(200),
                    if (node.clickable) "clickable" else null,
                    if (node.enabled) "enabled" else null
                ).joinToString(" | ")
            }

    private const val MAX_SUMMARY_NODES = 120
}
