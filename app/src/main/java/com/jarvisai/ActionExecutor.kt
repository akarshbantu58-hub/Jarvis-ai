package com.jarvisai

import android.content.Context
import android.net.Uri
import com.jarvisai.core.CommandPolicy
import com.jarvisai.core.CommandRisk

/**
 * Executes only the app's small, validated action vocabulary.
 * Call from a background thread: WAIT deliberately blocks the calling thread.
 * Accessibility still requires explicit user enablement in Android Settings.
 */
class ActionExecutor(context: Context) {
    private val appContext = context.applicationContext
    private val gestures = GestureController()
    private val intents = IntentDispatcher(appContext)
    private val verifier = ActionStateVerifier()
    private val policy = CommandPolicy()

    data class Result(
        val status: ExecutionSession.Status,
        val messages: List<String>,
        val screenSummary: String? = null
    )

    fun execute(command: String, steps: List<ActionStep>): Result {
        if (command.isBlank() || steps.isEmpty() || steps.size > MAX_STEPS) {
            return Result(ExecutionSession.Status.FAILED, listOf("No valid action plan was supplied."))
        }
        if (command.length > MAX_COMMAND_LENGTH) {
            return Result(ExecutionSession.Status.FAILED, listOf("Command is too long."))
        }

        when (policy.classify(command)) {
            CommandRisk.DANGEROUS -> return Result(
                ExecutionSession.Status.WAITING_FOR_CONFIRMATION,
                listOf("Blocked: this request is dangerous and cannot be executed by JARVIS.")
            )
            CommandRisk.SENSITIVE -> return Result(
                ExecutionSession.Status.WAITING_FOR_CONFIRMATION,
                listOf("Confirmation required. No actions were executed.")
            )
            CommandRisk.SAFE -> Unit
        }

        val session = ExecutionSession()
        session.begin(command, steps)
        val messages = mutableListOf<String>()
        var screen: String? = null

        for (step in steps) {
            val ok = runCatching {
                when (step.action) {
                    ActionStep.Action.OPEN_APP -> openAllowedApp(step.value)
                    ActionStep.Action.OPEN_URL -> openSafeUrl(step.value)
                    ActionStep.Action.OPEN_SETTINGS -> intents.openSettings()
                    ActionStep.Action.CLICK_TEXT -> step.value?.let(gestures::clickText) ?: false
                    ActionStep.Action.TYPE_TEXT -> step.value?.takeIf { it.length <= MAX_VALUE_LENGTH }
                        ?.let(gestures::typeText) ?: false
                    ActionStep.Action.SCROLL_FORWARD -> gestures.scrollForward()
                    ActionStep.Action.SCROLL_BACKWARD -> gestures.scrollBackward()
                    ActionStep.Action.GO_BACK -> gestures.back()
                    ActionStep.Action.GO_HOME -> gestures.home()
                    ActionStep.Action.READ_SCREEN -> {
                        val root = JarvisAccessibilityService.instance?.snapshot()
                        screen = PromptTemplates.screenSummary(root)
                        root != null
                    }
                    ActionStep.Action.SWIPE -> {
                        val a = step.x1; val b = step.y1; val c = step.x2; val d = step.y2
                        if (a == null || b == null || c == null || d == null ||
                            listOf(a, b, c, d).any { !it.isFinite() || it !in 0f..1f }) false
                        else {
                            val metrics = appContext.resources.displayMetrics
                            gestures.swipe(a * metrics.widthPixels, b * metrics.heightPixels,
                                c * metrics.widthPixels, d * metrics.heightPixels, step.durationMs)
                        }
                    }
                    ActionStep.Action.SPEAK -> false // TTS is handled by the voice layer, not this executor.
                    ActionStep.Action.WAIT -> false // Avoid blocking the UI or sleeping without an async scheduler.
                }
            }.getOrDefault(false)

            messages += verifier.verify(step, ok)
            if (!ok) {
                session.fail()
                return Result(ExecutionSession.Status.FAILED, messages, screen)
            }
            session.advance()
        }
        return Result(session.lastStatus, messages, screen)
    }

    private fun openAllowedApp(request: String?): Boolean {
        val key = request?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
        val packageName = APP_PACKAGES[key] ?: return false
        return intents.openPackage(packageName)
    }

    private fun openSafeUrl(value: String?): Boolean {
        val uri = runCatching { Uri.parse(value?.trim().orEmpty()) }.getOrNull() ?: return false
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return false
        return intents.openUrl(uri.toString())
    }

    companion object {
        private const val MAX_STEPS = 8
        private const val MAX_COMMAND_LENGTH = 1000
        private const val MAX_VALUE_LENGTH = 300
        private val APP_PACKAGES = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "maps" to "com.google.android.apps.maps"
        )
    }
}
