package com.jarvisai

import android.content.Context
import com.jarvisai.ai.AiProviderManager

object ApiClient {
    fun ask(context: Context, prompt: String, callback: (String) -> Unit) {
        if (prompt.isBlank()) {
            callback("Please tell me what you want to know.")
            return
        }

        val manager = AiProviderManager(context)
        if (!manager.isConfigured()) {
            callback("No valid AI provider is configured. Open API HUB and paste a supported API key.")
            return
        }

        manager.ask(prompt) { result ->
            callback(result.getOrElse { error ->
                "AI request failed: ${error.message ?: "unknown provider error"}"
            })
        }
    }
}
