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
            c == "open settings" || c == "open android settings" -> { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Opening Android settings." }
            c.contains("open youtube") -> { openUrl("https://www.youtube.com"); "Opening YouTube." }
            c.contains("open google") || c.contains("open browser") || c.contains("open chrome") -> { openUrl("https://www.google.com"); "Opening the browser." }
            c.contains("open camera") -> { context.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Opening camera." }
            c.contains("open whatsapp") -> launchPackage("com.whatsapp", "WhatsApp")
            c.contains("open instagram") -> launchPackage("com.instagram.android", "Instagram")
            c.contains("open maps") || c.contains("open google maps") -> { openUrl("https://maps.google.com"); "Opening Maps." }
            else -> null
        }
    }
    private fun openUrl(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    private fun launchPackage(pkg: String, name: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); return "Opening $name." }
        return "$name is not installed."
    }
}
