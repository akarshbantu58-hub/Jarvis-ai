package com.jarvisai

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import java.util.Locale

/**
 * Deterministic Android action boundary. It delegates UI automation only to the
 * user-enabled AccessibilityService and never bypasses Android permissions.
 */
class AppActionEngine(private val context: Context) {
    private val planner = AgentPlanner()
    private val gestures = GestureController()
    private val verifier = ActionStateVerifier()

    fun execute(command: String): String? {
        val c = command.lowercase(Locale.getDefault()).trim()
        val planned = planner.plan(command)
        if (planned.isNotEmpty()) return executePlan(planned)

        return when {
            c == "open settings" || c == "open android settings" -> {
                launch(Intent(Settings.ACTION_SETTINGS))
                "Opening Android settings."
            }
            c == "go home" || c == "go to home" -> {
                if (gestures.home()) "Going home."
                else {
                    launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                    "Going home."
                }
            }
            c == "go back" || c == "press back" -> {
                if (gestures.back()) "Going back."
                else "Back control requires JARVIS Accessibility Service to be enabled."
            }
            c.contains("open youtube") -> openPackageOrUrl("com.google.android.youtube", "YouTube", "https://www.youtube.com")
            c.contains("open chrome") -> openPackageOrUrl("com.android.chrome", "Chrome", "https://www.google.com")
            c.contains("open google") || c.contains("open browser") -> {
                openUrl("https://www.google.com")
                "Opening the browser."
            }
            c.contains("open camera") -> {
                launch(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
                "Opening camera."
            }
            c.contains("open whatsapp") -> launchPackage("com.whatsapp", "WhatsApp")
            c.contains("open instagram") -> launchPackage("com.instagram.android", "Instagram")
            c.contains("open maps") || c.contains("open google maps") ->
                openPackageOrUrl("com.google.android.apps.maps", "Google Maps", "https://maps.google.com")
            c.contains("turn on flashlight") || c.contains("turn flashlight on") -> setFlashlight(true)
            c.contains("turn off flashlight") || c.contains("turn flashlight off") -> setFlashlight(false)
            c.contains("battery") -> batteryStatus()
            c.contains("volume up") || c.contains("increase volume") -> adjustVolume(AudioManager.ADJUST_RAISE)
            c.contains("volume down") || c.contains("decrease volume") -> adjustVolume(AudioManager.ADJUST_LOWER)
            c.contains("read my notifications") || c.contains("read notifications") -> readNotifications()
            c.contains("read screen") || c.contains("what is on my screen") -> readScreen()
            c == "stop speaking" || c == "be quiet" -> "Speech stop is handled by the voice controller."
            else -> null
        }
    }

    private fun executePlan(steps: List<ActionStep>): String {
        val session = ExecutionSession().also { it.begin("planned", steps) }
        val results = mutableListOf<String>()
        for (step in steps) {
            val success = when (step.action) {
                ActionStep.Action.GO_HOME -> gestures.home()
                ActionStep.Action.GO_BACK -> gestures.back()
                ActionStep.Action.CLICK_TEXT -> gestures.clickText(step.value.orEmpty())
                ActionStep.Action.TYPE_TEXT -> gestures.typeText(step.value.orEmpty())
                ActionStep.Action.SCROLL_FORWARD -> gestures.scrollForward()
                ActionStep.Action.SCROLL_BACKWARD -> gestures.scrollBackward()
                ActionStep.Action.SWIPE -> gestures.swipe(
                    step.x1 ?: 0f, step.y1 ?: 0f, step.x2 ?: 0f, step.y2 ?: 0f, step.durationMs
                )
                ActionStep.Action.READ_SCREEN -> true
                else -> false
            }
            if (step.action == ActionStep.Action.READ_SCREEN) {
                results += readScreen()
                session.advance()
                continue
            }
            results += verifier.verify(step, success)
            if (!success) {
                session.fail()
                break
            }
            session.advance()
        }
        return results.joinToString("\n")
    }

    private fun readScreen(): String {
        val root = JarvisAccessibilityService.instance?.snapshot()
            ?: return "Screen access requires JARVIS Accessibility Service to be enabled."
        val summary = PromptTemplates.screenSummary(root)
        return if (summary.isBlank()) "The current screen has no readable accessibility text." else summary
    }

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun openPackageOrUrl(pkg: String, name: String, fallbackUrl: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            launch(intent)
            return "Opening $name."
        }
        openUrl(fallbackUrl)
        return "Opening $name in the browser."
    }

    private fun launchPackage(pkg: String, name: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            launch(intent)
            return "Opening $name."
        }
        return "$name is not installed."
    }

    private fun batteryStatus(): String {
        val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return "Battery information is unavailable."
        val percent = (level * 100 / scale).coerceIn(0, 100)
        return "Battery is at $percent percent."
    }

    private fun adjustVolume(direction: Int): String {
        val audio = context.getSystemService(AudioManager::class.java) ?: return "Audio controls are unavailable."
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        return if (direction == AudioManager.ADJUST_RAISE) "Volume increased." else "Volume decreased."
    }

    private fun setFlashlight(enabled: Boolean): String {
        val cameraManager = context.getSystemService(CameraManager::class.java)
            ?: return "Flashlight control is unavailable."
        val cameraId = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        if (cameraId == null) return "No flashlight is available on this device."
        return runCatching {
            cameraManager.setTorchMode(cameraId, enabled)
            if (enabled) "Flashlight turned on." else "Flashlight turned off."
        }.getOrElse { "Android did not allow flashlight control." }
    }

    private fun readNotifications(): String {
        if (!PermissionCenter.isNotificationAccessEnabled(context)) {
            return "Notification access is disabled. Enable it in JARVIS Permission Center first."
        }
        val items = NotificationStore.snapshot()
        if (items.isEmpty()) return "There are no recent notification summaries available."
        return items.take(5).joinToString("\n") { item ->
            val title = item.title.ifBlank { item.packageName }
            if (item.text.isBlank()) title else "$title: ${item.text}"
        }
    }
}
