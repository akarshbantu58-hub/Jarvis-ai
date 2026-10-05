package com.jarvisai

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional notification bridge. Android Settings must explicitly grant access.
 * JARVIS does not persist notification content here.
 */
class JarvisNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
        sendBroadcast(android.content.Intent(ACTION_NOTIFICATION).apply {
            setPackage(packageName)
            putExtra(EXTRA_PACKAGE, sbn.packageName)
            putExtra(EXTRA_TITLE, title.take(200))
            putExtra(EXTRA_TEXT, text.take(500))
        })
    }

    companion object {
        const val ACTION_NOTIFICATION = "com.jarvisai.action.NOTIFICATION"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
