package com.jarvisai

/**
 * Detects well-known API-key formats locally, without sending the key anywhere.
 *
 * Detection is intentionally conservative: an unknown key is never probed against
 * random providers because that would disclose a secret to an unintended service.
 */
object ApiKeyAutoDetector {
    data class Detection(
        val provider: String,
        val baseUrl: String,
        val model: String,
        val message: String
    )

    fun detect(rawKey: String): Detection? {
        val key = rawKey.trim()
        if (key.isBlank()) return null

        return when {
            key.startsWith("AIza", ignoreCase = false) -> Detection(
                provider = "Gemini",
                baseUrl = "",
                model = "gemini-3.8-flash",
                message = "Gemini API key detected."
            )

            key.startsWith("sk-or-v1-") -> Detection(
                provider = "OpenRouter",
                baseUrl = "https://openrouter.ai/api/v1/chat/completions",
                model = "openai/gpt-4o-mini",
                message = "OpenRouter API key detected."
            )

            key.startsWith("gsk_") -> Detection(
                provider = "Groq",
                baseUrl = "https://api.groq.com/openai/v1/chat/completions",
                model = "llama-3.3-70b-versatile",
                message = "Groq API key detected."
            )

            key.startsWith("sk-proj-") || key.startsWith("sk-") -> Detection(
                provider = "OpenAI-compatible",
                baseUrl = "https://api.openai.com/v1/chat/completions",
                model = "gpt-4o-mini",
                message = "OpenAI-style API key detected."
            )

            else -> null
        }
    }
}
