package com.jarvisai

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import com.jarvisai.ai.AiProviderManager

class ApiHubActivity : Activity() {
    private lateinit var providerPicker: Spinner
    private lateinit var endpointField: EditText
    private lateinit var modelField: EditText
    private lateinit var keyField: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 28)
            setBackgroundColor(Color.rgb(5, 8, 13))
        }

        fun label(text: String, color: Int = Color.CYAN) = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color)
            setPadding(0, 18, 0, 6)
        }

        fun edit(value: String, hintText: String, secret: Boolean = false) = EditText(this).apply {
            setText(value)
            hint = hintText
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            setPadding(12, 10, 12, 10)
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        root.addView(TextView(this).apply {
            text = "JARVIS AI HUB"
            textSize = 28f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Choose a provider, save its settings, then test the connection. Each provider has its own encrypted key."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 8)
        })

        root.addView(label("AI PROVIDER"))
        providerPicker = Spinner(this)
        providerPicker.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ApiHub.providers()
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        root.addView(providerPicker)

        root.addView(label("API ENDPOINT URL"))
        endpointField = edit("", "HTTPS endpoint or localhost URL")
        root.addView(endpointField)

        root.addView(label("MODEL ID"))
        modelField = edit("", "Exact model ID from provider")
        root.addView(modelField)

        root.addView(label("API KEY"))
        keyField = edit("", "Paste API key (leave blank to keep saved key)", true)
        root.addView(keyField)

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 16, 0, 8)
        }
        root.addView(statusText)

        fun selectedProvider(): String =
            providerPicker.selectedItem?.toString() ?: ApiHub.GEMINI_PROVIDER

        fun loadProvider(provider: String) {
            val config = runCatching { ApiHub.load(this, provider) }
                .getOrElse { ApiHub.defaultConfig(provider) }
            endpointField.setText(config.baseUrl)
            modelField.setText(config.model)
            keyField.setText("")
            statusText.text = if (config.apiKey.isNotBlank()) {
                "${config.provider}: saved key found • key hidden for safety"
            } else {
                "${config.provider}: no saved key"
            }
        }

        providerPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadProvider(ApiHub.providers()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        providerPicker.setSelection(ApiHub.providers().indexOf(ApiHub.load(this).provider).coerceAtLeast(0))

        root.addView(Button(this).apply {
            text = "SAVE PROVIDER SETTINGS"
            setOnClickListener {
                val provider = selectedProvider()
                val endpoint = endpointField.text.toString().trim()
                val model = modelField.text.toString().trim()
                val enteredKey = keyField.text.toString().trim()
                val savedKey = runCatching { ApiHub.load(this@ApiHubActivity, provider).apiKey }.getOrDefault("")
                val keyAvailable = enteredKey.isNotBlank() || savedKey.isNotBlank()
                val isLocal = runCatching {
                    val host = android.net.Uri.parse(endpoint).host.orEmpty()
                    host.equals("localhost", true) || host == "127.0.0.1" || host == "::1"
                }.getOrDefault(false)

                if (endpoint.isBlank() || model.isBlank()) {
                    Toast.makeText(this@ApiHubActivity, "Enter both endpoint URL and model ID.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (!endpoint.startsWith("https://", true) && !isLocal) {
                    Toast.makeText(this@ApiHubActivity, "Use HTTPS for remote API endpoints. HTTP is allowed only for localhost.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (!keyAvailable && !isLocal) {
                    Toast.makeText(this@ApiHubActivity, "Enter an API key for this provider.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val config = ApiHub.Config(provider, endpoint, enteredKey, model)
                runCatching { ApiHub.save(this@ApiHubActivity, config) }
                    .onSuccess {
                        keyField.setText("")
                        statusText.text = "$provider settings saved • key encrypted on this device"
                        Toast.makeText(this@ApiHubActivity, "Provider settings saved.", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { error ->
                        Toast.makeText(this@ApiHubActivity, "Could not save settings: ${error.message ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                    }
            }
        })

        root.addView(Button(this).apply {
            text = "TEST AI CONNECTION"
            setOnClickListener {
                val provider = selectedProvider()
                val endpoint = endpointField.text.toString().trim()
                val model = modelField.text.toString().trim()
                val enteredKey = keyField.text.toString().trim()
                val savedKey = runCatching { ApiHub.load(this@ApiHubActivity, provider).apiKey }.getOrDefault("")
                val isLocal = runCatching {
                    val host = android.net.Uri.parse(endpoint).host.orEmpty()
                    host.equals("localhost", true) || host == "127.0.0.1" || host == "::1"
                }.getOrDefault(false)

                if (endpoint.isBlank() || model.isBlank()) {
                    Toast.makeText(this@ApiHubActivity, "Enter endpoint URL and model ID first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (enteredKey.isBlank() && savedKey.isBlank() && !isLocal) {
                    Toast.makeText(this@ApiHubActivity, "Enter an API key before testing.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                runCatching {
                    ApiHub.save(this@ApiHubActivity, ApiHub.Config(provider, endpoint, enteredKey, model))
                }.onFailure {
                    Toast.makeText(this@ApiHubActivity, "Save settings first: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                statusText.text = "Testing $provider…"
                isEnabled = false
                AiProviderManager(this@ApiHubActivity).ask(
                    "Reply with exactly: JARVIS connection successful"
                ) { result ->
                    isEnabled = true
                    val message = result.fold(
                        { response -> "CONNECTED • $provider\n${response.take(240)}" },
                        { error -> "CONNECTION FAILED • $provider\n${error.message ?: error.javaClass.simpleName}" }
                    )
                    statusText.text = message
                    Toast.makeText(
                        this@ApiHubActivity,
                        if (result.isSuccess) "AI connection successful." else "AI connection failed. Read the status below.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        root.addView(Button(this).apply {
            text = "CLEAR SELECTED PROVIDER KEY"
            setOnClickListener {
                val provider = selectedProvider()
                runCatching { ApiHub.clear(this@ApiHubActivity, provider) }
                    .onSuccess {
                        keyField.setText("")
                        statusText.text = "$provider key cleared."
                        Toast.makeText(this@ApiHubActivity, "Saved key cleared.", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(this@ApiHubActivity, "Could not clear key.", Toast.LENGTH_LONG).show()
                    }
            }
        })

        root.addView(TextView(this).apply {
            text = "Providers use their own API billing, quotas and model availability. Never paste API keys into GitHub, screenshots or chat. For custom/local endpoints, use HTTPS unless the host is localhost."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 20, 0, 12)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
