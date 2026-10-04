package com.jarvisai

import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(buildUi()); refresh() }
    private fun buildUi() = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(32,40,32,32); setBackgroundColor(Color.rgb(5,8,13))
        addView(TextView(this@PermissionActivity).apply { text="JARVIS PERMISSION CENTER"; textSize=26f; setTextColor(Color.WHITE) })
        addView(TextView(this@PermissionActivity).apply { text="Permissions are requested only when required. Android controls all sensitive access."; textSize=15f; setTextColor(Color.LTGRAY); setPadding(0,18,0,24) })
        status=TextView(this@PermissionActivity).apply { textSize=15f; setTextColor(Color.CYAN) }; addView(status)
        addView(Button(this@PermissionActivity).apply { text="GRANT NEEDED PERMISSIONS"; setOnClickListener { PermissionCenter.requestMissing(permissionLauncher,this@PermissionActivity) } })
        addView(Button(this@PermissionActivity).apply { text="DISPLAY OVER OTHER APPS"; setOnClickListener { openOverlaySettings() } })
        addView(Button(this@PermissionActivity).apply { text="MAKE JARVIS DEFAULT ASSISTANT"; setOnClickListener { PermissionCenter.requestAssistantRole(this@PermissionActivity,9001) } })
        addView(Button(this@PermissionActivity).apply { text="OPEN ANDROID APP PERMISSIONS"; setOnClickListener { PermissionCenter.openAppDetails(this@PermissionActivity) } })
        addView(Button(this@PermissionActivity).apply { text="API HUB"; setOnClickListener { startActivity(Intent(this@PermissionActivity,ApiHubActivity::class.java)) } })
        addView(Button(this@PermissionActivity).apply { text="AUTOMATIONS"; setOnClickListener { startActivity(Intent(this@PermissionActivity,AutomationActivity::class.java)) } })
    }
    private fun openOverlaySettings() { if (Settings.canDrawOverlays(this)) { startService(Intent(this,JarvisOverlayService::class.java)); refresh() } else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }
    private fun refresh() { val missing=PermissionCenter.missingPermissions(this); val assistant=PermissionCenter.hasAssistantRole(this); val overlay=Settings.canDrawOverlays(this); status.text="Runtime: ${if(missing.isEmpty()) "READY" else "${missing.size} needed"}\nAssistant: ${if(assistant) "JARVIS" else "Not selected"}\nOverlay: ${if(overlay) "ENABLED" else "Not enabled"}" }
    override fun onResume(){ super.onResume(); if(::status.isInitialized) refresh() }
}
