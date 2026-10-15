package com.jarvisai

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/**
 * Floating JARVIS entry point. The title is draggable, the position is saved,
 * and short text prompts can be sent through the existing configured AI provider.
 */
class JarvisOverlayService : Service() {
    companion object {
        const val ACTION_SHOW_WAKE = "com.jarvisai.action.SHOW_WAKE_OVERLAY"
        const val ACTION_HIDE = "com.jarvisai.action.HIDE_OVERLAY"
        private const val POSITION_PREFS = "jarvis_overlay_position"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var view: LinearLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var responseView: TextView? = null
    private var promptInput: EditText? = null

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
            setPadding(dp(16), dp(12), dp(16), dp(12))
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
            text = "⠿  JARVIS  ·  DRAG TO MOVE"
            textSize = 14f
            setTextColor(Color.WHITE)
            letterSpacing = 0.025f
            gravity = Gravity.CENTER
            contentDescription = "Drag this header to move JARVIS."
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val subtitle = TextView(this).apply {
            text = "Ready when you are"
            textSize = 11f
            setTextColor(0xFF7DD3FC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        panel.addView(title)
        panel.addView(subtitle)

        val reply = TextView(this).apply {
            text = "Ask JARVIS a question below."
            textSize = 12f
            setTextColor(0xFFE2F5FF.toInt())
            gravity = Gravity.START
            setPadding(dp(8), dp(6), dp(8), dp(8))
            maxLines = 4
        }
        responseView = reply
        panel.addView(reply, LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.WRAP_CONTENT))

        val entryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = "Ask Gemini…"
            textSize = 14f
            maxLines = 1
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF94A3B8.toInt())
            setPadding(dp(10), 0, dp(10), 0)
            background = GradientDrawable().apply {
                setColor(0xFF101E2D.toInt())
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), 0xFF36566B.toInt())
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) enableKeyboardFocus()
            }
        }
        promptInput = input
        val send = Button(this).apply {
            text = "SEND"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF087EA4.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { sendPrompt() }
        }
        entryRow.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginEnd = dp(6)
        })
        entryRow.addView(send, LinearLayout.LayoutParams(dp(76), dp(48)))
        panel.addView(entryRow)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
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
        val prefs = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = max(0, prefs.getInt(KEY_X, dp(24)))
            y = max(dp(24), prefs.getInt(KEY_Y, dp(54)))
        }
        attachDragHandle(title, panel, params)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm?.addView(panel, params)
        overlayParams = params
        view = panel
    }

    private fun enableKeyboardFocus() {
        val panel = view ?: return
        val params = overlayParams ?: return
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        runCatching { wm?.updateViewLayout(panel, params) }
    }

    private fun sendPrompt() {
        val prompt = promptInput?.text?.toString()?.trim().orEmpty()
        if (prompt.isBlank()) {
            responseView?.text = "Type a question first."
            return
        }
        responseView?.text = "JARVIS is thinking…"
        promptInput?.text?.clear()
        ApiClient.ask(applicationContext, prompt) { answer ->
            mainHandler.post {
                if (view != null) {
                    responseView?.text = answer
                    val input = promptInput
                    input?.let {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(it.windowToken, 0)
                        it.clearFocus()
                    }
                    val panel = view
                    val params = overlayParams
                    if (panel != null && params != null) {
                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        runCatching { wm?.updateViewLayout(panel, params) }
                    }
                }
            }
        }
    }

    private fun attachDragHandle(
        handle: View,
        panel: View,
        params: WindowManager.LayoutParams
    ) {
        var startRawX = 0f
        var startRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()
                    if (kotlin.math.abs(dx) > dp(3) || kotlin.math.abs(dy) > dp(3)) moved = true
                    if (moved) {
                        val metrics = resources.displayMetrics
                        val maxX = max(0, metrics.widthPixels - panel.width)
                        val maxY = max(dp(24), metrics.heightPixels - panel.height)
                        params.x = min(max(0, startX + dx), maxX)
                        params.y = min(max(dp(24), startY + dy), maxY)
                        runCatching { wm?.updateViewLayout(panel, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).edit()
                        .putInt(KEY_X, params.x).putInt(KEY_Y, params.y).apply()
                    true
                }
                else -> false
            }
        }
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
        overlayParams = null
        responseView = null
        promptInput = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
