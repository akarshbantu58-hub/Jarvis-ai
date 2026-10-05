package com.jarvisai.core

/** Observable states for the JARVIS voice/AI experience. */
enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    ERROR,
    OFFLINE
}
