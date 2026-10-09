package com.jarvisai

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*

class ApiHubActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = runCatching { ApiHub.load(this) }.getOrElse {
            ApiHub.Config(ApiHub.GEMINI_PROVIDER, "https://generativelanguage.googleapis.com/v1beta", "", ApiHub.DEFAULT_MODEL)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 28)
            setBackgroundColor(Color.rgb(5, 8, 13))
        }

        fun edit(value: String, hint: String, secret: Boolean = false) = EditText(this).apply {
            setText(value)
            this.hint = hint
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        root.addView(TextView(this).apply {
            text = "JARVIS AI HUB"
            textSize = 28f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "GEMINI API"
            textSize = 16f
            setTextColor(Color.CYAN)
            setPadding(0, 24, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Paste your Google Gemini API key. JARVIS uses Gemini only. Your key is encrypted on this device and is not shown in messages."
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 14)
        })

        val quickKey = edit("", "Paste Gemini API key here", true)
        root.addView(quickKey)

        val detected = TextView(this).apply {
            text = if (config.apiKey.isNotBlank()) "Gemini configured • ${config.model}" else "No Gemini API key configured"
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
        }
        root.addView(detected)

        root.addView(Button(this).apply {
            text = "SAVE GEMINI KEY"
            setOnClickListener {
                val keyValue = quickKey.text.toString().trim()
                if (keyValue.isBlank()) {
                    Toast.makeText(this@ApiHubActivity, "Paste your Gemini API key first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!keyValue.startsWith("AIza")) {
                    Toast.makeText(this@ApiHubActivity, "That does not look like a Gemini API key. Check Google AI Studio.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val result = runCatching { ApiHub.autoConfigure(this@ApiHubActivity, keyValue) }
                result.onSuccess { detection ->
                    if (detection == null) {
                        Toast.makeText(this@ApiHubActivity, "Gemini key format not recognized. Nothing was saved.", Toast.LENGTH_LONG).show()
                    } else {
                        quickKey.setText("")
                        detected.text = "Gemini configured • ${ApiHub.DEFAULT_MODEL}"
                        Toast.makeText(this@ApiHubActivity, "Gemini key saved securely on this device.", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this@ApiHubActivity, "Could not save key: ${error.javaClass.simpleName}. Try again; JARVIS is still open.", Toast.LENGTH_LONG).show()
                }
            }
        })

        val advancedTitle = TextView(this).apply {
            text = "GEMINI MODEL"
            textSize = 16f
            setTextColor(Color.CYAN)
            setPadding(0, 24, 0, 8)
        }
        root.addView(advancedTitle)
        val advanced = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(advanced)
        val model = edit(ApiHub.DEFAULT_MODEL, "Gemini model ID")
        advanced.addView(model)
        advanced.addView(Button(this).apply {
            text = "SAVE MODEL"
            setOnClickListener {
                Toast.makeText(this@ApiHubActivity, "Model is currently fixed to ${ApiHub.DEFAULT_MODEL} to keep the Gemini endpoint consistent.", Toast.LENGTH_LONG).show()
                model.setText(ApiHub.DEFAULT_MODEL)
            }
        })
        advancedTitle.setOnClickListener {
            advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        root.addView(TextView(this).apply {
            text = "If saving fails, JARVIS will show an error instead of closing. Never share your API key in screenshots, chat, or GitHub."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 20, 0, 12)
        })
        root.addView(Button(this).apply {
            text = "CLEAR GEMINI KEY"
            setOnClickListener {
                runCatching { ApiHub.clear(this@ApiHubActivity) }
                    .onSuccess {
                        quickKey.setText("")
                        detected.text = "No Gemini API key configured"
                        Toast.makeText(this@ApiHubActivity, "Gemini key cleared", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(this@ApiHubActivity, "Could not clear key. Please try again.", Toast.LENGTH_LONG).show()
                    }
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
