package com.jarvisai.ai

import android.os.Handler
import android.os.Looper
import com.jarvisai.ApiHub
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class OpenAiCompatibleProvider : AiProvider {
    override val id: String = "openai-compatible"
    override val displayName: String = "OpenAI-compatible"

    override fun isConfigured(config: ApiHub.Config): Boolean =
        config.baseUrl.isNotBlank() && config.model.isNotBlank()

    override fun ask(config: ApiHub.Config, prompt: String, callback: (Result<String>) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val result = runCatching {
                val endpoint = URL(config.baseUrl)
                require(endpoint.protocol.equals("https", true) ||
                    (endpoint.protocol.equals("http", true) && endpoint.host in setOf("localhost", "127.0.0.1", "::1"))) {
                    "API endpoint must use HTTPS; HTTP is allowed only for localhost."
                }
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                }.toString()
                val connection = endpoint.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doInput = true
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                if (config.apiKey.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
                try {
                    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) error("API $code: ${safeError(text)}")
                    val json = JSONObject(text)
                    json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                        ?.takeIf { it.isNotBlank() }
                        ?: json.optString("output_text").takeIf { it.isNotBlank() }
                        ?: error("The provider returned no text.")
                } finally {
                    connection.disconnect()
                }
            }
            handler.post { callback(result) }
        }.start()
    }

    private fun safeError(body: String): String = body
        .replace(Regex("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*[^,\\s]+"), "$1: [REDACTED]")
        .take(500)
}
