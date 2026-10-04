package com.jarvisai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = intent.getStringExtra("command") ?: return
        val hour = intent.getIntExtra("hour", 9)
        val minute = intent.getIntExtra("minute", 0)

        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "jarvis_automations"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "JARVIS Automations", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("JARVIS Automation")
            .setContentText(command)
            .setAutoCancel(true)
            .build()
        manager.notify(command.hashCode(), notification)

        // Schedule the next daily occurrence without requiring an exact-alarm permission.
        AutomationScheduler.scheduleDaily(context, hour, minute, command)
    }
}
