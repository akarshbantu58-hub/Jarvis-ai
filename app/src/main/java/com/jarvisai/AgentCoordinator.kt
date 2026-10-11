package com.jarvisai

import android.content.Context

/**
 * Coordinates Gemini planning and strict parsing. This class never executes
 * actions; the caller must apply safety policy and obtain confirmation where
 * needed before passing a plan to ActionExecutor.
 */
object AgentCoordinator {
    data class PlanResult(
        val steps: List<ActionStep>,
        val explanation: String
    )

    fun plan(
        context: Context,
        command: String,
        callback: (PlanResult) -> Unit
    ) {
        val cleanCommand = command.trim()
        if (cleanCommand.isEmpty() || cleanCommand.length > MAX_COMMAND_LENGTH) {
            callback(PlanResult(emptyList(), "Command is empty or too long."))
            return
        }

        val screen = JarvisAccessibilityService.instance?.snapshot()
        val summary = PromptTemplates.screenSummary(screen)
        val prompt = buildString {
            append(PromptTemplates.UI_ACTION_PLANNER)
            append("\n\nUSER COMMAND:\n")
            append(cleanCommand.take(MAX_COMMAND_LENGTH))
            append("\n\nCURRENT ACCESSIBILITY TREE:\n")
            append(summary.ifBlank { "(No accessible screen available. Do not invent controls.)" })
            append("\n\nReturn JSON only. Schema: {\"actions\":[{\"action\":\"ACTION_NAME\",\"value\":\"optional\"}]} ")
        }

        ApiClient.ask(context, prompt) { response ->
            val steps = ActionPlanParser.parse(response)
            if (steps.isEmpty()) {
                callback(PlanResult(emptyList(), "Gemini did not return a valid supported action plan. No action was executed."))
            } else {
                callback(PlanResult(steps, "Gemini produced a plan with ${steps.size} action(s). Review and safety checks are still required."))
            }
        }
    }

    private const val MAX_COMMAND_LENGTH = 1000
}
