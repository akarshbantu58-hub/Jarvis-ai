package com.jarvisai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        refresh()
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 40, 32, 32)
        setBackgroundColor(android.graphics.Color.rgb(5, 8, 13))

        addView(TextView(this@PermissionActivity).apply {
            text = "JARVIS PERMISSION CENTER"
            textSize = 26f
            setTextColor(android.graphics.Color.WHITE)
        })
        addView(TextView(this@PermissionActivity).apply {
            text = "JARVIS only asks for permissions when a feature needs them. Nothing is silently granted."
            textSize = 15f
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, 18, 0, 24)
        })
        status = TextView(this@PermissionActivity).apply { textSize = 15f; setTextColor(android.graphics.Color.CYAN) }
        addView(status)
        addView(Button(this@PermissionActivity).apply {
            text = "GRANT NEEDED PERMISSIONS"
            setOnClickListener { PermissionCenter.requestMissing(permissionLauncher, this@PermissionActivity) }
        })
        addView(Button(this@PermissionActivity).apply {
            text = "MAKE JARVIS DEFAULT ASSISTANT"
            setOnClickListener { PermissionCenter.requestAssistantRole(this@PermissionActivity, 9001) }
        })
        addView(Button(this@PermissionActivity).apply {
            text = "OPEN ANDROID APP PERMISSIONS"
            setOnClickListener { PermissionCenter.openAppDetails(this@PermissionActivity) }
        })
        addView(Button(this@PermissionActivity).apply {
            text = "AUTOMATIONS"
            setOnClickListener { startActivity(Intent(this@PermissionActivity, AutomationActivity::class.java)) }
        })
    }

    private fun refresh() {
        val missing = PermissionCenter.missingPermissions(this)
        val assistant = PermissionCenter.hasAssistantRole(this)
        status.text = "Runtime permissions: ${if (missing.isEmpty()) "READY" else "${missing.size} needed"}\nDefault assistant: ${if (assistant) "JARVIS" else "Not selected"}"
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }
}
