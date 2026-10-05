package com.jarvisai

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional notification bridge. Android Settings must explicitly grant access.
 * JARVIS keeps only a small in-memory summary and does not persist notification content here.
 */
class JarvisNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString().orEmpty().take(200)
        val text = extras?.getCharSequence("android.text")?.toString().orEmpty().take(500)
        NotificationStore.add(NotificationStore.Item(sbn.packageName, title, text))
        sendBroadcast(android.content.Intent(ACTION_NOTIFICATION).apply {
            setPackage(packageName)
            putExtra(EXTRA_PACKAGE, sbn.packageName)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        })
    }

    companion object {
        const val ACTION_NOTIFICATION = "com.jarvisai.action.NOTIFICATION"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
