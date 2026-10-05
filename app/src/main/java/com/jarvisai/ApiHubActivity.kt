package com.jarvisai

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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
            text = "Gemini and OpenAI-compatible providers. API keys stay encrypted in Android Keystore storage."
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 20)
        })

        val provider = edit(config.provider, "Provider: Gemini / OpenAI-compatible / Local")
        val url = edit(config.baseUrl, "Base URL (leave blank for Gemini default)")
        val key = edit("", if (config.apiKey.isBlank()) "API Key" else "API Key saved securely — leave blank to keep it", true)
        val model = edit(config.model, "Model")
        listOf(provider, url, key, model).forEach(root::addView)

        root.addView(TextView(this).apply {
            text = "Examples:\nGemini → model: gemini-2.5-flash, URL can be blank\nOpenAI-compatible → full /v1/chat/completions URL\nLocal → HTTPS or localhost HTTP endpoint"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 16, 0, 12)
        })

        root.addView(Button(this).apply {
            text = "SAVE API CONFIG"
            setOnClickListener {
                val enteredKey = key.text.toString().trim()
                val finalKey = if (enteredKey.isBlank()) config.apiKey else enteredKey
                ApiHub.save(this@ApiHubActivity, ApiHub.Config(
                    provider.text.toString(),
                    url.text.toString(),
                    finalKey,
                    model.text.toString()
                ))
                Toast.makeText(this@ApiHubActivity, "Saved securely", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "CLEAR CONFIG"
            setOnClickListener {
                ApiHub.clear(this@ApiHubActivity)
                key.setText("")
                Toast.makeText(this@ApiHubActivity, "AI configuration cleared", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(root)
    }
}
