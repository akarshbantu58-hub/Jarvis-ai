package com.jarvisai

/** One discrete, inspectable step in a JARVIS execution plan. */
data class ActionStep(
    val action: Action,
    val value: String? = null,
    val x1: Float? = null,
    val y1: Float? = null,
    val x2: Float? = null,
    val y2: Float? = null,
    val durationMs: Long = 350L
) {
    enum class Action {
        OPEN_APP,
        OPEN_URL,
        OPEN_SETTINGS,
        CLICK_TEXT,
        TYPE_TEXT,
        SCROLL_FORWARD,
        SCROLL_BACKWARD,
        SWIPE,
        GO_BACK,
        GO_HOME,
        READ_SCREEN,
        SPEAK,
        WAIT
    }
}
