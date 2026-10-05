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

        val config = ApiHub.load(this)
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
            text = "QUICK CONNECT"
            textSize = 16f
            setTextColor(Color.CYAN)
            setPadding(0, 24, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "Paste an API key. JARVIS detects supported key formats locally and fills the provider, endpoint and model automatically. The key is encrypted with Android Keystore."
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 14)
        })

        val quickKey = edit("", "Paste API key here", true)
        root.addView(quickKey)

        val detected = TextView(this).apply {
            text = if (config.apiKey.isNotBlank()) {
                "Current: ${config.provider} • ${config.model}"
            } else {
                "No API key configured"
            }
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
        }
        root.addView(detected)

        root.addView(Button(this).apply {
            text = "AUTO-DETECT & SAVE"
            setOnClickListener {
                val key = quickKey.text.toString().trim()
                if (key.isBlank()) {
                    Toast.makeText(this@ApiHubActivity, "Paste an API key first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val result = ApiHub.autoConfigure(this@ApiHubActivity, key)
                if (result == null) {
                    detected.text = "Unknown key format. Nothing was sent anywhere. Use Advanced Setup for a custom OpenAI-compatible endpoint."
                    Toast.makeText(this@ApiHubActivity, "Provider could not be detected safely", Toast.LENGTH_LONG).show()
                } else {
                    detected.text = "Detected: ${result.provider} • ${result.model}"
                    Toast.makeText(this@ApiHubActivity, "${result.message} Saved securely.", Toast.LENGTH_LONG).show()
                }
            }
        })

        val advancedTitle = TextView(this).apply {
            text = "ADVANCED SETUP"
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

        val provider = edit(config.provider, "Provider")
        val url = edit(config.baseUrl, "Endpoint URL")
        val key = edit("", if (config.apiKey.isBlank()) "API Key" else "Saved API key — leave blank to keep it", true)
        val model = edit(config.model, "Model")
        listOf(provider, url, key, model).forEach(advanced::addView)

        advanced.addView(Button(this).apply {
            text = "SAVE ADVANCED CONFIG"
            setOnClickListener {
                val enteredKey = key.text.toString().trim()
                val finalKey = if (enteredKey.isBlank()) config.apiKey else enteredKey
                ApiHub.save(this@ApiHubActivity, ApiHub.Config(
                    provider.text.toString(),
                    url.text.toString(),
                    finalKey,
                    model.text.toString()
                ))
                detected.text = "Current: ${provider.text} • ${model.text}"
                Toast.makeText(this@ApiHubActivity, "Saved securely", Toast.LENGTH_SHORT).show()
            }
        })

        advancedTitle.setOnClickListener {
            advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        root.addView(TextView(this).apply {
            text = "Security: JARVIS does not probe unknown providers with your key. Unknown keys require manual endpoint configuration. Never paste an API key into GitHub, chat, logs, or screenshots."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 20, 0, 12)
        })

        root.addView(Button(this).apply {
            text = "CLEAR CONFIG"
            setOnClickListener {
                ApiHub.clear(this@ApiHubActivity)
                quickKey.setText("")
                detected.text = "No API key configured"
                Toast.makeText(this@ApiHubActivity, "AI configuration cleared", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(root)
    }
}
