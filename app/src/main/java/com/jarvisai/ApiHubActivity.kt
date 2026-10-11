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
            text = "CHOOSE GEMINI MODEL"
            textSize = 16f
            setTextColor(Color.CYAN)
            setPadding(0, 24, 0, 8)
        }
        root.addView(advancedTitle)

        val modelStatus = TextView(this).apply {
            text = "Tap LOAD MODELS to retrieve models available to your Gemini API key."
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 8)
        }
        root.addView(modelStatus)

        val modelSpinner = Spinner(this)
        root.addView(modelSpinner)
        val modelDetails = TextView(this).apply {
            setTextColor(Color.GRAY)
            setPadding(0, 6, 0, 8)
        }
        root.addView(modelDetails)

        var availableModels: List<GeminiModelCatalog.Model> = emptyList()
        fun displayModels(models: List<GeminiModelCatalog.Model>, selectedId: String) {
            availableModels = models
            val labels = models.map { "${it.displayName}  •  ${it.id}" }
            modelSpinner.adapter = ArrayAdapter(
                this@ApiHubActivity,
                android.R.layout.simple_spinner_dropdown_item,
                labels
            )
            val index = models.indexOfFirst { it.id == selectedId }.let { if (it >= 0) it else 0 }
            modelSpinner.setSelection(index)
            modelDetails.text = models.getOrNull(index)?.description.orEmpty()
            modelSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    modelDetails.text = availableModels.getOrNull(position)?.description.orEmpty()
                }
            }
        }

        root.addView(Button(this).apply {
            text = "LOAD ALL AVAILABLE GEMINI MODELS"
            setOnClickListener {
                val key = quickKey.text.toString().trim().ifBlank { runCatching { ApiHub.load(this@ApiHubActivity).apiKey }.getOrDefault("") }
                if (key.isBlank()) {
                    Toast.makeText(this@ApiHubActivity, "Save your Gemini API key first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                isEnabled = false
                modelStatus.text = "Loading models from Google…"
                GeminiModelCatalog.fetch(key) { result ->
                    isEnabled = true
                    result.onSuccess { models ->
                        displayModels(models, runCatching { ApiHub.load(this@ApiHubActivity).model }.getOrDefault(ApiHub.DEFAULT_MODEL))
                        modelStatus.text = "${models.size} Gemini models available to this API key."
                    }.onFailure { error ->
                        modelStatus.text = "Could not load models."
                        Toast.makeText(this@ApiHubActivity, error.message ?: "Gemini model request failed.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })

        root.addView(Button(this).apply {
            text = "SAVE SELECTED MODEL"
            setOnClickListener {
                val selected = availableModels.getOrNull(modelSpinner.selectedItemPosition)
                if (selected == null) {
                    Toast.makeText(this@ApiHubActivity, "Load the available models first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                runCatching { ApiHub.setModel(this@ApiHubActivity, selected.id) }
                    .onSuccess {
                        detected.text = "Gemini configured • ${selected.id}"
                        Toast.makeText(this@ApiHubActivity, "Gemini model saved: ${selected.id}", Toast.LENGTH_LONG).show()
                    }
                    .onFailure { Toast.makeText(this@ApiHubActivity, "Could not save model: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        })

        // Populate with the saved model so the selected configuration is visible immediately.
        runCatching {
            val saved = ApiHub.load(this)
            displayModels(listOf(GeminiModelCatalog.Model(saved.model, saved.model, "Currently saved model. Load models to browse Google's full list.")), saved.model)
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
