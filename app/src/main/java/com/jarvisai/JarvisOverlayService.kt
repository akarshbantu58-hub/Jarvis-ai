package com.jarvisai

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * User-visible floating entry point shown after the opted-in wake-word service
 * detects "Hey JARVIS". Talk opens the existing voice pipeline; Close dismisses
 * the panel. Overlay permission is required and is never granted silently.
 */
class JarvisOverlayService : Service() {
    companion object {
        const val ACTION_SHOW_WAKE = "com.jarvisai.action.SHOW_WAKE_OVERLAY"
        const val ACTION_HIDE = "com.jarvisai.action.HIDE_OVERLAY"
    }

    private var wm: WindowManager? = null
    private var view: LinearLayout? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching { showOverlay() }.onFailure { stopSelf() }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (view != null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(14), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF01A2B40.toInt(), 0xF009111C.toInt())
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0xFF38BDF8.toInt())
            }
            elevation = dp(18).toFloat()
        }

        val title = TextView(this).apply {
            text = "✦  JARVIS IS HERE"
            textSize = 14f
            setTextColor(Color.WHITE)
            letterSpacing = 0.04f
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Ready when you are"
            textSize = 11f
            setTextColor(0xFF7DD3FC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(10))
        }
        panel.addView(title)
        panel.addView(subtitle)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val talk = Button(this).apply {
            text = "TALK"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF087EA4.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(18), dp(4), dp(18), dp(4))
            setOnClickListener { openVoiceAssistant() }
        }
        val close = Button(this).apply {
            text = "CLOSE"
            isAllCaps = false
            setTextColor(0xFFBAE6FD.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF172536.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), 0xFF36566B.toInt())
            }
            setPadding(dp(14), dp(4), dp(14), dp(4))
            setOnClickListener { stopSelf() }
        }
        controls.addView(talk, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)
        ).apply { marginEnd = dp(8) })
        controls.addView(close, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)
        ))
        panel.addView(controls)

        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(54)
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm?.addView(panel, params)
        view = panel
    }

    private fun openVoiceAssistant() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("jarvis_wake_overlay_tap", true)
        }
        runCatching { startActivity(intent) }
        stopSelf()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
