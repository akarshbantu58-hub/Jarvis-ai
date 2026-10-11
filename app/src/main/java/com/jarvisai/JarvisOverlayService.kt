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
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small, user-visible floating entry point shown after the opted-in wake-word
 * service detects "Hey JARVIS". The user can tap it to open the full assistant.
 * Android's overlay permission is required and can never be granted silently.
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
            setPadding(dp(18), dp(12), dp(18), dp(12))
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
            text = "Tap to talk  •  Tap outside to keep using your app"
            textSize = 10f
            setTextColor(0xFF7DD3FC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        }
        panel.addView(title)
        panel.addView(subtitle)
        panel.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("jarvis_wake_overlay_tap", true)
            }
            runCatching { startActivity(intent) }
            stopSelf()
        }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
