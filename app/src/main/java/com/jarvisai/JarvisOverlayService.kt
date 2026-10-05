package com.jarvisai

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class JarvisOverlayService : Service() {
    private var wm: WindowManager? = null
    private var view: TextView? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching { showOverlay() }.onFailure { stopSelf() }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (view != null) return
        val v = TextView(this).apply {
            text = "JARVIS • READY"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD101A26.toInt())
            setPadding(28, 18, 28, 18)
            elevation = 16f
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 48
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm?.addView(v, params)
        view = v
    }

    override fun onDestroy() {
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
