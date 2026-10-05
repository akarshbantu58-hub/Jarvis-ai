package com.jarvisai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** System-level intent dispatcher. Android remains the authority over every launch. */
class IntentDispatcher(private val context: Context) {
    fun openPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return start(intent)
    }

    fun openUrl(url: String): Boolean = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun openSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    fun start(intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
