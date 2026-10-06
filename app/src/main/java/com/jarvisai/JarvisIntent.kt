package com.jarvisai

/** A normalized representation of a user request before execution. */
data class JarvisIntent(
    val rawCommand: String,
    val type: Type,
    val normalizedCommand: String = rawCommand.trim()
) {
    enum class Type {
        LOCAL_ACTION,
        AI_QUERY,
        OPEN_SETTINGS,
        OPEN_AUTOMATION,
        OPEN_API_HUB,
        DEFAULT_ASSISTANT,
        EMPTY
    }
}
