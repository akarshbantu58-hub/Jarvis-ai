package com.jarvisai

/** Lightweight post-condition checks for local Android actions. */
class ActionStateVerifier {
    fun verify(step: ActionStep, success: Boolean): String = when {
        success -> "${step.action.name} completed."
        step.action in setOf(ActionStep.Action.CLICK_TEXT, ActionStep.Action.TYPE_TEXT, ActionStep.Action.SWIPE,
            ActionStep.Action.SCROLL_FORWARD, ActionStep.Action.SCROLL_BACKWARD) ->
            "${step.action.name} was not completed. Accessibility Service may be disabled or the target may be unavailable."
        else -> "${step.action.name} was not completed."
    }
}
