package com.jarvisai

import org.json.JSONObject

/**
 * Strict parser for Gemini-produced action-plan JSON.
 * Parsing does not authorize execution; callers must apply CommandPolicy and
 * inspect the plan before passing it to ActionExecutor.
 */
object ActionPlanParser {
    private const val MAX_JSON_LENGTH = 12000
    private const val MAX_ACTIONS = 8
    private const val MAX_VALUE_LENGTH = 300

    fun parse(json: String): List<ActionStep> {
        if (json.isBlank() || json.length > MAX_JSON_LENGTH) return emptyList()
        return runCatching {
            val root = JSONObject(json)
            val actions = root.optJSONArray("actions") ?: return emptyList()
            if (actions.length() !in 1..MAX_ACTIONS) return emptyList()
            val result = ArrayList<ActionStep>(actions.length())

            for (i in 0 until actions.length()) {
                val item = actions.optJSONObject(i) ?: return emptyList()
                val name = item.optString("action", "").trim()
                val action = runCatching { ActionStep.Action.valueOf(name) }.getOrNull()
                    ?: return emptyList()
                val value = if (item.has("value") && !item.isNull("value")) {
                    item.optString("value", "").trim().takeIf {
                        it.isNotEmpty() && it.length <= MAX_VALUE_LENGTH
                    } ?: return emptyList()
                } else null

                val step = when (action) {
                    ActionStep.Action.OPEN_APP,
                    ActionStep.Action.OPEN_URL,
                    ActionStep.Action.CLICK_TEXT,
                    ActionStep.Action.TYPE_TEXT,
                    ActionStep.Action.SPEAK -> {
                        if (value == null) return emptyList()
                        ActionStep(action = action, value = value)
                    }
                    ActionStep.Action.SWIPE -> {
                        val x1 = item.optDouble("x1", Double.NaN).toFloat()
                        val y1 = item.optDouble("y1", Double.NaN).toFloat()
                        val x2 = item.optDouble("x2", Double.NaN).toFloat()
                        val y2 = item.optDouble("y2", Double.NaN).toFloat()
                        val duration = item.optLong("durationMs", 350L)
                        if (listOf(x1, y1, x2, y2).any { !it.isFinite() || it !in 0f..1f } ||
                            duration !in 50L..2000L) return emptyList()
                        ActionStep(action, x1 = x1, y1 = y1, x2 = x2, y2 = y2, durationMs = duration)
                    }
                    ActionStep.Action.WAIT -> return emptyList()
                    else -> {
                        if (value != null) return emptyList()
                        ActionStep(action = action)
                    }
                }
                result += step
            }
            result
        }.getOrDefault(emptyList())
    }
}
