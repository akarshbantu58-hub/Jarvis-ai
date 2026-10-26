package com.jarvisai

import android.app.AlertDialog
import android.app.Dialog
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
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private var speechManager: TextToSpeechManager? = null
    private var expanded = true
    private var expandedViews: List<View> = emptyList()
    private var collapseButton: Button? = null
    private var menuButton: Button? = null
    private val chatPrefs by lazy { getSharedPreferences("jarvis_saved_chats", MODE_PRIVATE) }

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
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "JARVIS floating assistant header. Drag to move."
        }
        val title = TextView(this).apply {
            text = "✦  JARVIS"
            textSize = 16f
            setTextColor(Color.WHITE)
            letterSpacing = 0.025f
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "Drag this header to move JARVIS."
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        val minimize = Button(this).apply {
            text = "−"
            textSize = 22f
            isAllCaps = false
            setTextColor(0xFFE0E7FF.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF26364A.toInt())
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(10), 0, dp(10), 0)
            contentDescription = "Collapse or expand JARVIS chat"
            setOnClickListener { toggleExpanded(panel) }
        }
        collapseButton = minimize
        val more = Button(this).apply {
            text = "⋮"
            textSize = 22f
            isAllCaps = false
            setTextColor(0xFFE0E7FF.toInt())
            background = GradientDrawable().apply {
                setColor(0x662B3C50)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), 0x5538BDF8)
            }
            contentDescription = "Previous chats, account settings and more"
            setOnClickListener { showAssistantMenu() }
        }
        menuButton = more
        header.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(more, LinearLayout.LayoutParams(dp(38), dp(40)).apply { marginEnd = dp(5) })
        header.addView(minimize, LinearLayout.LayoutParams(dp(42), dp(40)))
        panel.addView(header)
        val subtitle = TextView(this).apply {
            text = "Ready when you are"
            textSize = 11f
            setTextColor(0xFF7DD3FC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        panel.addView(subtitle)

        val reply = TextView(this).apply {
            text = "Ask JARVIS a question below."
            textSize = 12f
            setTextColor(0xFFE2F5FF.toInt())
            gravity = Gravity.START
            setPadding(dp(8), dp(6), dp(8), dp(8))
            maxLines = 8
            setLineSpacing(dp(2).toFloat(), 1.05f)
            background = GradientDrawable().apply {
                setColor(0xB3122030.toInt())
                cornerRadius = dp(16).toFloat()
            }
        }
        responseView = reply
        val replyScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            background = GradientDrawable().apply {
                setColor(0xB3122030.toInt())
                cornerRadius = dp(16).toFloat()
            }
            addView(reply, ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            ))
        }
        panel.addView(replyScroll, LinearLayout.LayoutParams(dp(280), dp(180)).apply {
            bottomMargin = dp(10)
        })

        val entryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = "Ask Gemini…"
            textSize = 14f
            maxLines = 4
            setSingleLine(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
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
            setOnEditorActionListener { _, actionId, event ->
                val sendAction = actionId == EditorInfo.IME_ACTION_SEND
                val enterAction = event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.action == android.view.KeyEvent.ACTION_DOWN &&
                    event.isCtrlPressed
                if (sendAction || enterAction) {
                    sendPrompt()
                    true
                } else false
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
        val speak = Button(this).apply {
            text = "SPEAK"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF34445A.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), 0xFF38BDF8.toInt())
            }
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                val answer = responseView?.text?.toString().orEmpty()
                if (answer.isNotBlank() &&
                    answer != "Ask JARVIS a question below." &&
                    answer != "JARVIS is thinking…" &&
                    answer != "Type a question first."
                ) {
                    if (speechManager == null) speechManager = TextToSpeechManager(applicationContext)
                    speechManager?.speak(answer)
                } else {
                    responseView?.text = "Ask a question and wait for the answer before tapping SPEAK."
                }
            }
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
        ).apply { marginEnd = dp(6) })
        controls.addView(speak, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)
        ).apply { marginEnd = dp(6) })
        controls.addView(close, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)
        ))
        panel.addView(controls)
        expandedViews = listOf(subtitle, reply, entryRow, controls)

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
        expanded = true
        updateExpandedUi(panel)
    }

    private fun toggleExpanded(panel: LinearLayout) {
        expanded = !expanded
        updateExpandedUi(panel)
    }

    private fun updateExpandedUi(panel: LinearLayout) {
        expandedViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
        panel.setPadding(
            dp(if (expanded) 16 else 8),
            dp(if (expanded) 12 else 4),
            dp(if (expanded) 16 else 8),
            dp(if (expanded) 12 else 4)
        )
        collapseButton?.text = if (expanded) "−" else "+"
        overlayParams?.let { params ->
            runCatching { wm?.updateViewLayout(panel, params) }
        }
    }

    private fun enableKeyboardFocus() {
        val panel = view ?: return
        val params = overlayParams ?: return
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        runCatching { wm?.updateViewLayout(panel, params) }
    }

    private fun sendPrompt() {
        if (!expanded) {
            view?.let { toggleExpanded(it) }
        }
        val prompt = promptInput?.text?.toString()?.trim().orEmpty()
        if (prompt.isBlank()) {
            responseView?.text = "Type a question first."
            return
        }
        responseView?.text = "JARVIS is thinking…"
        promptInput?.text?.clear()
        saveChatLine("You: $prompt")
        ApiClient.ask(applicationContext, prompt) { answer ->
            mainHandler.post {
                if (view != null) {
                    responseView?.text = answer
                    saveChatLine("JARVIS: $answer")
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

    private fun saveChatLine(line: String) {
        val old = chatPrefs.getString("history", "").orEmpty()
        val updated = (old.lines().filter { it.isNotBlank() } + line).takeLast(120)
        chatPrefs.edit().putString("history", updated.joinToString("\n")).putLong("last_saved_at", System.currentTimeMillis()).apply()
    }

    private fun showAssistantMenu() {
        val dialog = Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF01A2029.toInt(), 0xF00C121C.toInt(), 0xF01D1B2B.toInt())
            ).apply {
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), 0x667BE7FF)
            }
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 22f
            setTextColor(0xFFF2F7FF.toInt())
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        top.addView(brand, LinearLayout.LayoutParams(0, dp(52), 1f))
        val close = TextView(this).apply {
            text = "×"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFFE2F4FF.toInt())
            background = glassBackground(0x332C4356, 0x557BE7FF)
            setOnClickListener { dialog.dismiss() }
        }
        top.addView(close, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(top)
        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL AI"
            textSize = 10f
            letterSpacing = 0.18f
            setTextColor(0xFF7DDCFF.toInt())
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(subtitle)

        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(rows)
        fun section(label: String) {
            rows.addView(TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(0xFF91A4B9.toInt())
                setPadding(dp(8), dp(16), 0, dp(8))
            })
        }
        fun item(icon: String, label: String, description: String = "", action: () -> Unit) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(9), dp(10), dp(9))
                background = glassBackground(0x222F4053, 0x3349DFFF)
                isClickable = true
                isFocusable = true
                setOnClickListener { dialog.dismiss(); action() }
            }
            val glyph = TextView(this).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(0xFF8DEBFF.toInt())
            }
            card.addView(glyph, LinearLayout.LayoutParams(dp(38), dp(42)))
            val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            labels.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(0xFFF0F5FC.toInt())
            })
            if (description.isNotBlank()) labels.addView(TextView(this).apply {
                text = description
                textSize = 11f
                setTextColor(0xFFA2B2C5.toInt())
            })
            card.addView(labels, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            rows.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(7)
            })
        }
        section("WORKSPACE")
        item("＋", "New chat", "Start a fresh conversation") {
            chatPrefs.edit().remove("history").apply()
            responseView?.text = "New conversation started. Ask JARVIS anything."
        }
        item("◷", "Previous chats", "Saved on this device") { showSavedChats() }
        item("⚙", "Assistant settings", "Floating panel and preferences") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Floating assistant")
                .setMessage("Drag the JARVIS title to move the panel. Use − / + to collapse or expand it. Its position is saved on this device.")
                .setPositiveButton("OK", null).show()
        }
        section("ACCOUNT")
        item("◎", "Google account", "Sign-in configuration") { showAccountSettings() }
        item("↗", "Continue with Google", "OAuth setup required") { showGoogleSignInInfo() }
        section("J.A.R.V.I.S.")
        item("◈", "AI connection", "Uses the app's configured AI provider") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("AI connection")
                .setMessage("JARVIS sends prompts through the existing configured AI provider. Check the app's AI settings to configure your provider and API key.")
                .setPositiveButton("OK", null).show()
        }
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val profile = TextView(this).apply {
            text = "  ●   Local profile  ·  Chats stay on this device"
            textSize = 11f
            setTextColor(0xFF9DB3C8.toInt())
            setPadding(dp(8), dp(18), dp(8), dp(8))
        }
        root.addView(profile)
        dialog.setContentView(root)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.58f)
            window.setLayout((resources.displayMetrics.widthPixels * 0.84f).toInt(), resources.displayMetrics.heightPixels)
            window.setGravity(Gravity.START or Gravity.TOP)
            window.attributes = window.attributes.apply {
                width = (resources.displayMetrics.widthPixels * 0.84f).toInt()
                height = resources.displayMetrics.heightPixels
                gravity = Gravity.START or Gravity.TOP
            }
        }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.84f).toInt(), resources.displayMetrics.heightPixels)
        dialog.window?.setGravity(Gravity.START or Gravity.TOP)
    }

    private fun glassBackground(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(17).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun showSavedChats() {
        val history = chatPrefs.getString("history", "").orEmpty().trim()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Previous chats · This device")
            .setMessage(if (history.isBlank()) "No saved messages yet. Send a message to create your local chat history." else history)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear history") { _, _ -> chatPrefs.edit().remove("history").apply() }
            .show()
    }

    private fun showAccountSettings() {
        val signedIn = chatPrefs.getString("google_account_email", null)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Account")
            .setMessage(if (signedIn == null) "Not signed in. Chat history is saved locally on this device. Google sign-in needs the app's OAuth client configuration before secure account sign-in can be enabled." else "Signed in as $signedIn")
            .setPositiveButton("OK", null)
            .setNeutralButton("Google sign-in") { _, _ -> showGoogleSignInInfo() }
            .show()
    }

    private fun showGoogleSignInInfo() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Continue with Google")
            .setMessage("Google sign-in UI is planned, but secure authentication is not active yet. It requires a Google OAuth client ID configured for this Android package and signing certificate. This build does not pretend you are signed in.")
            .setPositiveButton("OK", null)
            .show()
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
        expandedViews = emptyList()
        collapseButton = null
        menuButton = null
        speechManager?.release()
        speechManager = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
