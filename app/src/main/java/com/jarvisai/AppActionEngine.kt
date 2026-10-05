package com.jarvisai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

class AppActionEngine(private val context: Context) {
    fun execute(command: String): String? {
        val c = command.lowercase(Locale.getDefault()).trim()
        return when {
            c == "open settings" || c == "open android settings" -> {
                launch(Intent(Settings.ACTION_SETTINGS))
                "Opening Android settings."
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
            else -> null
        }
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
}
