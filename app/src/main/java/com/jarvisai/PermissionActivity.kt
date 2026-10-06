package com.jarvisai

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        refresh()
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 40, 32, 32)
        setBackgroundColor(Color.rgb(5, 8, 13))
        addView(TextView(this@PermissionActivity).apply {
            text = "JARVIS PERMISSION CENTER"
            textSize = 26f
            setTextColor(Color.WHITE)
        })
        addView(TextView(this@PermissionActivity).apply {
            text = "JARVIS only uses sensitive capabilities after Android grants them. Accessibility, notification access, overlay, MediaProjection and device-admin are optional system-level settings."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 18, 0, 24)
        })
        status = TextView(this@PermissionActivity).apply { textSize = 15f; setTextColor(Color.CYAN) }
        addView(status)

        addButton("GRANT CAMERA / MICROPHONE / NOTIFICATIONS") { PermissionCenter.requestMissing(permissionLauncher, this@PermissionActivity) }
        addButton("ENABLE ACCESSIBILITY SERVICE") { PermissionCenter.openAccessibilitySettings(this@PermissionActivity) }
        addButton("ENABLE NOTIFICATION ACCESS") { PermissionCenter.openNotificationAccessSettings(this@PermissionActivity) }
        addButton("DISPLAY OVER OTHER APPS") {
            if (Settings.canDrawOverlays(this@PermissionActivity)) startService(Intent(this@PermissionActivity, JarvisOverlayService::class.java))
            else PermissionCenter.openOverlaySettings(this@PermissionActivity)
        }
        addButton("MAKE JARVIS DEFAULT ASSISTANT") { PermissionCenter.requestAssistantRole(this@PermissionActivity, 9001) }
        addButton("SCREEN CAPTURE / AI SCREEN CONTEXT") { startActivity(Intent(this@PermissionActivity, ScreenCaptureActivity::class.java)) }
        addButton("OPTIONAL DEVICE ADMINISTRATOR") { DeviceAdminHelper.request(this@PermissionActivity) }
        addButton("OPEN ANDROID APP PERMISSIONS") { PermissionCenter.openAppDetails(this@PermissionActivity) }
        addButton("API HUB") { startActivity(Intent(this@PermissionActivity, ApiHubActivity::class.java)) }
        addButton("MEMORY") { startActivity(Intent(this@PermissionActivity, MemoryActivity::class.java)) }
        addButton("AUTOMATIONS") { startActivity(Intent(this@PermissionActivity, AutomationActivity::class.java)) }
        addButton("SYSTEM FEATURE CENTER") { startActivity(Intent(this@PermissionActivity, FeatureCenterActivity::class.java)) }
    }

    private fun LinearLayout.addButton(label: String, action: () -> Unit) {
        addView(Button(this@PermissionActivity).apply { text = label; setOnClickListener { action() } })
    }

    private fun refresh() {
        val missing = PermissionCenter.missingPermissions(this)
        val assistant = PermissionCenter.hasAssistantRole(this)
        val accessibility = PermissionCenter.isAccessibilityEnabled(this)
        val notifications = PermissionCenter.isNotificationAccessEnabled(this)
        val overlay = Settings.canDrawOverlays(this)
        status.text = buildString {
            append("Runtime permissions: "); append(if (missing.isEmpty()) "READY" else "${missing.size} needed")
            append("\nDefault assistant: "); append(if (assistant) "JARVIS OS" else "Not selected")
            append("\nAccessibility: "); append(if (accessibility) "ENABLED" else "Not enabled")
            append("\nNotification access: "); append(if (notifications) "ENABLED" else "Not enabled")
            append("\nOverlay: "); append(if (overlay) "ENABLED" else "Not enabled")
            append("\nDevice admin: "); append(if (DeviceAdminHelper.isEnabled(this@PermissionActivity)) "ENABLED" else "Not enabled")
        }
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }
}
