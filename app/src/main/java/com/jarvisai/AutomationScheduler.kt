package com.jarvisai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AutomationScheduler {
    private const val REQUEST_CODE = 4107
    private const val ACTION = "com.jarvisai.AUTOMATION"

    fun scheduleDaily(context: Context, hour: Int, minute: Int, command: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AutomationReceiver::class.java).apply {
            action = ACTION
            putExtra("command", command)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    fun cancel(context: Context) {
        val intent = Intent(context, AutomationReceiver::class.java).setAction(ACTION)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }
}
