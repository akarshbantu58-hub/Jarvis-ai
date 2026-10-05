package com.jarvisai

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * User-started microphone foreground service for wake-word mode.
 * Android controls when this service may start and the user can stop it from the notification.
 */
class JarvisVoiceForegroundService : Service() {
    companion object {
        const val ACTION_START_WAKE = "com.jarvisai.action.START_WAKE"
        const val ACTION_STOP_WAKE = "com.jarvisai.action.STOP_WAKE"
        const val ACTION_WAKE_DETECTED = "com.jarvisai.action.WAKE_DETECTED"
        private const val CHANNEL_ID = "jarvis_voice"
        private const val NOTIFICATION_ID = 2401
    }

    private var wakeWordEngine: WakeWordEngine? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_WAKE -> {
                stopWakeMode()
                return START_NOT_STICKY
            }
            ACTION_START_WAKE -> startWakeMode()
            else -> return START_NOT_STICKY
        }
        // Never resurrect microphone capture after Android kills the service.
        // Wake-word mode must always be explicitly started by the user.
        return START_NOT_STICKY
    }

    private fun startWakeMode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        val notification = buildNotification("Listening for “Hey JARVIS”")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (wakeWordEngine == null) {
            wakeWordEngine = WakeWordEngine(
                context = this,
                onWakeWord = {
                    val detected = Intent(ACTION_WAKE_DETECTED).setPackage(packageName)
                    sendBroadcast(detected)
                    updateNotification("Wake word detected — JARVIS is ready")
                },
                onError = { message -> updateNotification(message) }
            )
        }
        wakeWordEngine?.start()
    }

    private fun stopWakeMode() {
        wakeWordEngine?.stop()
        wakeWordEngine = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            2402,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2403,
            Intent(this, JarvisVoiceForegroundService::class.java).setAction(ACTION_STOP_WAKE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS OS")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "JARVIS voice", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows when JARVIS wake-word mode is active."
                }
            )
        }
    }

    override fun onDestroy() {
        wakeWordEngine?.stop()
        wakeWordEngine = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
