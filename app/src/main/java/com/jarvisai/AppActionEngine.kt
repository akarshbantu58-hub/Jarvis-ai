package com.jarvisai

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import java.util.Locale

/** Android action boundary using only user-authorized Android APIs. */
class AppActionEngine(private val context: Context) {
    private val planner = AgentPlanner()
    private val gestures = GestureController()
    private val verifier = ActionStateVerifier()

    fun execute(command: String): String? {
        val c = command.lowercase(Locale.getDefault()).trim()
        val planned = planner.plan(command)
        if (planned.isNotEmpty()) return executePlan(planned)

        return when {
            c == "open settings" || c == "open android settings" -> { launch(Intent(Settings.ACTION_SETTINGS)); "Opening Android settings." }
            c == "go home" || c == "go to home" -> if (gestures.home()) "Going home." else { launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)); "Going home." }
            c == "go back" || c == "press back" -> if (gestures.back()) "Going back." else "Back control requires JARVIS Accessibility Service to be enabled."
            c.contains("open youtube") -> openPackageOrUrl("com.google.android.youtube", "YouTube", "https://www.youtube.com")
            c.contains("open chrome") -> openPackageOrUrl("com.android.chrome", "Chrome", "https://www.google.com")
            c.contains("open google") || c.contains("open browser") -> { openUrl("https://www.google.com"); "Opening the browser." }
            c.contains("open camera") || c.contains("take a photo") || c.contains("take photo") -> { launch(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)); "Opening camera." }
            c.contains("open whatsapp") -> launchPackage("com.whatsapp", "WhatsApp")
            c.contains("open instagram") -> launchPackage("com.instagram.android", "Instagram")
            c.contains("open maps") || c.contains("open google maps") -> openPackageOrUrl("com.google.android.apps.maps", "Google Maps", "https://maps.google.com")
            c.contains("turn on flashlight") || c.contains("turn flashlight on") -> setFlashlight(true)
            c.contains("turn off flashlight") || c.contains("turn flashlight off") -> setFlashlight(false)
            c.contains("battery") -> batteryStatus()
            c.contains("volume up") || c.contains("increase volume") -> adjustVolume(AudioManager.ADJUST_RAISE)
            c.contains("volume down") || c.contains("decrease volume") -> adjustVolume(AudioManager.ADJUST_LOWER)
            c.contains("bluetooth") -> bluetoothStatusOrSettings(c.contains("on") || c.contains("enable"))
            c.contains("do not disturb") -> dndSettings(c.contains("on") || c.contains("enable"))
            c.contains("network") || c.contains("internet connection") || c.contains("wifi status") -> networkStatus()
            c.contains("set an alarm") || c.contains("set alarm") -> openAlarm()
            c.contains("call ") || c.startsWith("dial ") -> dialNumber(extractAfter(c, if (c.startsWith("dial ")) "dial " else "call "))
            c.contains("send message") || c.contains("send sms") -> messageIntent(c)
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
                ActionStep.Action.SWIPE -> gestures.swipe(step.x1 ?: 0f, step.y1 ?: 0f, step.x2 ?: 0f, step.y2 ?: 0f, step.durationMs)
                ActionStep.Action.READ_SCREEN -> true
                else -> false
            }
            if (step.action == ActionStep.Action.READ_SCREEN) {
                results += readScreen()
                session.advance()
                continue
            }
            results += verifier.verify(step, success)
            if (!success) { session.fail(); break }
            session.advance()
        }
        return results.joinToString("\n")
    }

    private fun readScreen(): String {
        val root = JarvisAccessibilityService.instance?.snapshot() ?: return "Screen access requires JARVIS Accessibility Service to be enabled."
        val summary = PromptTemplates.screenSummary(root)
        return if (summary.isBlank()) "The current screen has no readable accessibility text." else summary
    }

    private fun readNotifications(): String {
        val items = NotificationStore.snapshot()
        if (items.isEmpty()) return "No recent notifications are available. Enable JARVIS notification access in Android settings if you want notification reading."
        return buildString {
            append("Recent notifications:\n")
            items.take(10).forEachIndexed { index, item ->
                append(index + 1).append(". ")
                if (item.title.isNotBlank()) append(item.title) else append(item.packageName)
                if (item.text.isNotBlank()) append(": ").append(item.text)
                append('\n')
            }
        }.trim()
    }

    private fun launch(intent: Intent?) {
        val safeIntent = intent ?: return
        safeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(safeIntent)
    }

    private fun openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun openPackageOrUrl(pkg: String, name: String, fallbackUrl: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (intent != null) { launch(intent); "Opening $name." }
        else { openUrl(fallbackUrl); "Opening $name in the browser." }
    }

    private fun launchPackage(pkg: String, name: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (intent != null) { launch(intent); "Opening $name." } else "$name is not installed."
    }

    private fun batteryStatus(): String {
        val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return "Battery information is unavailable."
        val percent = (level * 100 / scale).coerceIn(0, 100)
        val charging = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1) in setOf(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL)
        return "Battery is at $percent percent${if (charging) " and charging" else ""}."
    }

    private fun adjustVolume(direction: Int): String {
        val audio = context.getSystemService(AudioManager::class.java) ?: return "Audio controls are unavailable."
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        return if (direction == AudioManager.ADJUST_RAISE) "Volume increased." else "Volume decreased."
    }

    private fun setFlashlight(enabled: Boolean): String {
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return "Flashlight control is unavailable."
        val cameraId = runCatching { cameraManager.cameraIdList.firstOrNull { id -> cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } }.getOrNull()
        if (cameraId == null) return "No flashlight is available on this device."
        return runCatching { cameraManager.setTorchMode(cameraId, enabled); if (enabled) "Flashlight turned on." else "Flashlight turned off." }.getOrElse { "Android did not allow flashlight control." }
    }

    private fun bluetoothStatusOrSettings(wantOn: Boolean): String {
        val adapter = runCatching { android.bluetooth.BluetoothAdapter.getDefaultAdapter() }.getOrNull() ?: return "Bluetooth is unavailable."
        if (wantOn && !adapter.isEnabled) { launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return "Android requires you to enable Bluetooth in system settings." }
        return if (adapter.isEnabled) "Bluetooth is on." else "Bluetooth is off."
    }

    private fun dndSettings(wantOn: Boolean): String {
        val manager = context.getSystemService(android.app.NotificationManager::class.java) ?: return "Do Not Disturb control is unavailable."
        if (!manager.isNotificationPolicyAccessGranted) { launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); return "Notification policy access is required. Enable it in Android settings." }
        launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        return if (wantOn) "Opening Do Not Disturb access. Android controls the final state." else "Opening Do Not Disturb settings."
    }

    private fun networkStatus(): String {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return "Network information is unavailable."
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        return when {
            caps == null -> "The tablet appears to be offline."
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Connected to Wi-Fi."
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected through mobile data."
            else -> "A network connection is active."
        }
    }

    private fun openAlarm(): String {
        launch(Intent(AlarmClock.ACTION_SET_ALARM).apply { putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS alarm") })
        return "Opening Android alarm setup."
    }

    private fun dialNumber(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) return "Tell me the phone number to dial."
        launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
        return "Opening the dialer for $cleaned."
    }

    private fun messageIntent(command: String): String {
        val body = command.substringAfter("send message", "").substringAfter("send sms", "").trim()
        launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply { putExtra("sms_body", body) })
        return "Opening the messaging app. Android will require you to review and send the message."
    }

    private fun extractAfter(value: String, marker: String): String = value.substringAfter(marker, "").trim()
}
