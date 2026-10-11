package com.jarvisai

import java.util.Locale

/**
 * Converts common spoken requests into a small, inspectable action plan.
 * This planner is deliberately deterministic: model output is never executed
 * directly. Unsupported or ambiguous requests return an empty plan.
 */
class AgentPlanner {
    fun plan(command: String): List<ActionStep> {
        val original = command.trim()
        if (original.isBlank() || original.length > MAX_COMMAND_LENGTH) return emptyList()
        val normalized = original.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

        return when {
            normalized == "go home" || normalized == "go to home" ->
                listOf(step(ActionStep.Action.GO_HOME))
            normalized == "go back" || normalized == "press back" ->
                listOf(step(ActionStep.Action.GO_BACK))
            normalized in setOf("scroll down", "scroll forward") ->
                listOf(step(ActionStep.Action.SCROLL_FORWARD))
            normalized in setOf("scroll up", "scroll backward") ->
                listOf(step(ActionStep.Action.SCROLL_BACKWARD))
            normalized in setOf("read screen", "what is on my screen", "describe my screen") ->
                listOf(step(ActionStep.Action.READ_SCREEN))
            normalized.startsWith("click ") || normalized.startsWith("tap ") ->
                valueStep(ActionStep.Action.CLICK_TEXT, original.substringAfter(' ').trim())
            normalized.startsWith("type ") ->
                valueStep(ActionStep.Action.TYPE_TEXT, original.substringAfter(' ').trim())
            normalized.startsWith("open settings") ->
                listOf(step(ActionStep.Action.OPEN_SETTINGS))
            normalized.startsWith("open https://") || normalized.startsWith("open http://") ->
                valueStep(ActionStep.Action.OPEN_URL, original.substringAfter(' ').trim())
            normalized.startsWith("open ") ->
                valueStep(ActionStep.Action.OPEN_APP, original.substringAfter(' ').trim())
            else -> emptyList()
        }.let(::validate)
    }

    /** Keep plans short and reject missing or oversized action arguments. */
    private fun validate(steps: List<ActionStep>): List<ActionStep> {
        if (steps.isEmpty() || steps.size > MAX_STEPS) return emptyList()
        if (steps.any { it.value != null && (it.value.isBlank() || it.value.length > MAX_VALUE_LENGTH) }) {
            return emptyList()
        }
        return steps
    }

    private fun valueStep(action: ActionStep.Action, value: String): List<ActionStep> =
        if (value.isBlank()) emptyList() else listOf(step(action, value))

    private fun step(action: ActionStep.Action, value: String? = null) = ActionStep(action = action, value = value)

    companion object {
        private const val MAX_COMMAND_LENGTH = 1000
        private const val MAX_VALUE_LENGTH = 300
        private const val MAX_STEPS = 8
    }
}
