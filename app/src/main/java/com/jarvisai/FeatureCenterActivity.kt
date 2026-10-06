package com.jarvisai

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Central, explicit entry point for advanced JARVIS capabilities. */
class FeatureCenterActivity : AppCompatActivity() {
    private lateinit var modeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(28, 40, 28, 32)
        setBackgroundColor(Color.rgb(4, 8, 14))
        addView(TextView(this@FeatureCenterActivity).apply { text = "JARVIS OS • SYSTEM CENTER"; textSize = 26f; setTextColor(Color.CYAN); gravity = Gravity.CENTER_HORIZONTAL })
        addView(TextView(this@FeatureCenterActivity).apply {
            text = "Advanced capabilities are opt-in. Android permission dialogs and system settings remain authoritative."
            textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 16, 0, 18)
        })
        modeText = TextView(this@FeatureCenterActivity).apply { setTextColor(Color.CYAN); textSize = 15f }
        addView(modeText)
        addButton("BATTERY SAVER MODE") { RuntimeSettings.setMode(this@FeatureCenterActivity, RuntimeSettings.Mode.SAVER); refreshMode() }
        addButton("NORMAL MODE") { RuntimeSettings.setMode(this@FeatureCenterActivity, RuntimeSettings.Mode.NORMAL); refreshMode() }
        addButton("PERFORMANCE MODE") { RuntimeSettings.setMode(this@FeatureCenterActivity, RuntimeSettings.Mode.PERFORMANCE); refreshMode() }
        addButton("TOGGLE REDUCED MOTION") { RuntimeSettings.setReduceMotion(this@FeatureCenterActivity, !RuntimeSettings.reduceMotion(this@FeatureCenterActivity)); refreshMode() }
        addButton("TOGGLE MOTION BUBBLES") { RuntimeSettings.setBubbles(this@FeatureCenterActivity, !RuntimeSettings.bubbles(this@FeatureCenterActivity)); refreshMode() }
        addButton("CAMERA • CAPTURE + AI VISION") { startActivity(Intent(this@FeatureCenterActivity, CameraActivity::class.java)) }
        addButton("SCREEN CAPTURE • MEDIAPROJECTION") { startActivity(Intent(this@FeatureCenterActivity, ScreenCaptureActivity::class.java)) }
        addButton("MEMORY • VIEW / EDIT / DELETE") { startActivity(Intent(this@FeatureCenterActivity, MemoryActivity::class.java)) }
        addButton("DEVELOPER STUDIO • SAFE AI CODING") { startActivity(Intent(this@FeatureCenterActivity, DeveloperStudioActivity::class.java)) }
        addButton("PERMISSION CENTER") { startActivity(Intent(this@FeatureCenterActivity, PermissionActivity::class.java)) }
        addButton("API HUB • GEMINI / COMPATIBLE") { startActivity(Intent(this@FeatureCenterActivity, ApiHubActivity::class.java)) }
        addButton("DEVICE ADMIN • OPTIONAL") { DeviceAdminHelper.request(this@FeatureCenterActivity) }
        addButton("DO NOT DISTURB ACCESS") { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        addButton("OVERLAY / FLOATING JARVIS") {
            if (Settings.canDrawOverlays(this@FeatureCenterActivity)) startService(Intent(this@FeatureCenterActivity, JarvisOverlayService::class.java))
            else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName")))
        }
        refreshMode()
    }

    private fun refreshMode() {
        if (!::modeText.isInitialized) return
        modeText.text = "MODE: ${RuntimeSettings.mode(this)} • REDUCED MOTION: ${RuntimeSettings.reduceMotion(this)} • BUBBLES: ${RuntimeSettings.bubbles(this)}"
    }

    private fun LinearLayout.addButton(label: String, action: () -> Unit) {
        addView(Button(this@FeatureCenterActivity).apply { text = label; setOnClickListener { action() } })
    }
}
