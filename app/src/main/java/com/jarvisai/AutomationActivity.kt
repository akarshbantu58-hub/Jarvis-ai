package com.jarvisai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AutomationActivity : AppCompatActivity() {
    private lateinit var timeInput: EditText
    private lateinit var commandInput: EditText
    private lateinit var status: TextView

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || Build.VERSION.SDK_INT < 33) saveAutomationInternal()
        else status.text = "Notifications are disabled. Enable them in Android Settings to receive automation alerts."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(28, 36, 28, 28)
        setBackgroundColor(android.graphics.Color.rgb(5, 8, 13))
        addView(TextView(this@AutomationActivity).apply {
            text = "JARVIS AUTOMATIONS"
            textSize = 28f
            setTextColor(android.graphics.Color.WHITE)
        })
        addView(TextView(this@AutomationActivity).apply {
            text = "Create a daily task. JARVIS schedules it locally and posts a notification at the chosen time."
            textSize = 14f
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, 12, 0, 18)
        })
        timeInput = EditText(this@AutomationActivity).apply {
            hint = "Time (HH:mm), e.g. 07:30"
            setSingleLine(true)
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        addView(timeInput)
        commandInput = EditText(this@AutomationActivity).apply {
            hint = "Automation, e.g. Remind me to study physics"
            setSingleLine(false)
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        addView(commandInput)
        addView(Button(this@AutomationActivity).apply {
            text = "SAVE DAILY AUTOMATION"
            setOnClickListener { requestNotificationThenSave() }
        })
        addView(Button(this@AutomationActivity).apply {
            text = "CANCEL AUTOMATION"
            setOnClickListener { AutomationScheduler.cancel(this@AutomationActivity); status.text = "Automation cancelled." }
        })
        status = TextView(this@AutomationActivity).apply {
            textSize = 15f
            setTextColor(android.graphics.Color.CYAN)
            setPadding(0, 20, 0, 0)
        }
        addView(status)
    }

    private fun requestNotificationThenSave() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else saveAutomationInternal()
    }

    private fun saveAutomationInternal() {
        val parts = timeInput.text.toString().trim().split(":")
        val command = commandInput.text.toString().trim()
        if (parts.size != 2 || command.isBlank()) {
            status.text = "Enter a valid time and automation command."
            return
        }
        val hour = parts[0].toIntOrNull()
        val minute = parts[1].toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            status.text = "Time must be HH:mm, from 00:00 to 23:59."
            return
        }
        AutomationScheduler.scheduleDaily(this, hour, minute, command)
        status.text = "Saved: daily at %02d:%02d — %s".format(hour, minute, command)
    }
}
