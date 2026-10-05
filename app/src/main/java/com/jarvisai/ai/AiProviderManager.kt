package com.jarvisai.ai

import android.content.Context
import com.jarvisai.ApiHub

/**
 * Runtime router for the provider selected/configured by the user.
 *
 * Provider discovery happens locally in ApiKeyAutoDetector. This class is
 * responsible only for routing an already configured key to the correct
 * transport implementation.
 */
class AiProviderManager(private val context: Context) {
    private val providers: List<AiProvider> = listOf(
        GeminiProvider(),
        OpenAiCompatibleProvider()
    )

    private val config: ApiHub.Config
        get() = ApiHub.load(context)

    fun isConfigured(): Boolean = selectedProvider()?.isConfigured(config) == true

    fun configuredProviderName(): String? =
        selectedProvider()?.takeIf { it.isConfigured(config) }?.displayName

    fun configuration(): ApiHub.Config = config

    fun ask(prompt: String, callback: (Result<String>) -> Unit) {
        if (prompt.isBlank()) {
            callback(Result.failure(IllegalArgumentException("Prompt is empty.")))
            return
        }

        val current = config
        val provider = selectedProvider()
        if (provider == null || !provider.isConfigured(current)) {
            callback(Result.failure(IllegalStateException("No valid AI provider is configured.")))
            return
        }

        provider.ask(current, prompt, callback)
    }

    private fun selectedProvider(): AiProvider? {
        val id = config.provider.trim().lowercase()
        return when {
            id.contains("gemini") -> providers.firstOrNull { it.id == "gemini" }
            id.contains("openrouter") -> providers.firstOrNull { it.id == "openai-compatible" }
            id.contains("groq") -> providers.firstOrNull { it.id == "openai-compatible" }
            id.contains("openai") || id.contains("compatible") || id.contains("local") ->
                providers.firstOrNull { it.id == "openai-compatible" }
            else -> null
        }
    }
}
