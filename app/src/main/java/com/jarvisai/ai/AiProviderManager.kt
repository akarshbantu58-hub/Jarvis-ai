package com.jarvisai.ai

import android.content.Context
import com.jarvisai.ApiHub

/** Chooses only providers explicitly configured by the user. */
class AiProviderManager(private val context: Context) {
    private val providers: List<AiProvider> = listOf(
        GeminiProvider(),
        OpenAiCompatibleProvider()
    )

    private val config: ApiHub.Config
        get() = ApiHub.load(context)

    fun isConfigured(): Boolean = selectedProvider()?.isConfigured(config) == true

    fun configuredProviderName(): String? = selectedProvider()?.takeIf { it.isConfigured(config) }?.displayName

    fun configuration(): ApiHub.Config = config

    fun ask(prompt: String, callback: (Result<String>) -> Unit) {
        val current = config
        val provider = selectedProvider()
        if (provider == null || !provider.isConfigured(current)) {
            callback(Result.failure(IllegalStateException("No configured AI provider.")))
            return
        }
        provider.ask(current, prompt, callback)
    }

    private fun selectedProvider(): AiProvider? {
        val id = config.provider.trim().lowercase()
        return when {
            id.contains("gemini") -> providers.first { it.id == "gemini" }
            id.contains("local") || id.contains("openai") || id.contains("compatible") -> providers.first { it.id == "openai-compatible" }
            else -> providers.firstOrNull { it.id == "openai-compatible" }
        }
    }
}
