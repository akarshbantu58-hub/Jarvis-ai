package com.jarvisai

import java.util.Locale

/** Deterministic first-stage planner. AI planning can be plugged in later. */
class AgentPlanner {
    fun plan(command: String): List<ActionStep> {
        val c = command.trim().lowercase(Locale.ROOT)
        if (c.isBlank()) return emptyList()
        return when {
            c == "go home" || c == "go to home" -> listOf(ActionStep(ActionStep.Action.GO_HOME))
            c == "go back" || c == "press back" -> listOf(ActionStep(ActionStep.Action.GO_BACK))
            c == "scroll down" || c == "scroll forward" -> listOf(ActionStep(ActionStep.Action.SCROLL_FORWARD))
            c == "scroll up" || c == "scroll backward" -> listOf(ActionStep(ActionStep.Action.SCROLL_BACKWARD))
            c.startsWith("click ") -> listOf(ActionStep(ActionStep.Action.CLICK_TEXT, command.substringAfter(' ', "").trim()))
            c.startsWith("tap ") -> listOf(ActionStep(ActionStep.Action.CLICK_TEXT, command.substringAfter(' ', "").trim()))
            c.startsWith("type ") -> listOf(ActionStep(ActionStep.Action.TYPE_TEXT, command.substringAfter(' ', "").trim()))
            c == "read screen" || c == "what is on my screen" -> listOf(ActionStep(ActionStep.Action.READ_SCREEN))
            else -> emptyList()
        }
    }
}
