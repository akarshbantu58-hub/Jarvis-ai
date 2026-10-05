package com.jarvisai.ai

import android.os.Handler
import android.os.Looper
import com.jarvisai.ApiHub
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GeminiProvider : AiProvider {
    override val id: String = "gemini"
    override val displayName: String = "Gemini"

    override fun isConfigured(config: ApiHub.Config): Boolean =
        config.apiKey.isNotBlank() && config.model.isNotBlank()

    override fun ask(config: ApiHub.Config, prompt: String, callback: (Result<String>) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val result = runCatching {
                val configuredBase = config.baseUrl.trimEnd('/')
                val base = if (configuredBase.isBlank() || configuredBase.contains("api.openai.com")) {
                    "https://generativelanguage.googleapis.com/v1beta"
                } else {
                    configuredBase
                }
                val endpoint = if (base.contains("{model}")) {
                    base.replace("{model}", config.model)
                } else {
                    "$base/models/${config.model}:generateContent"
                }
                val encodedKey = URLEncoder.encode(config.apiKey, StandardCharsets.UTF_8.name())
                val url = URL("$endpoint?key=$encodedKey")
                require(url.protocol.equals("https", true)) { "Gemini endpoint must use HTTPS." }
                val body = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    ))
                }.toString()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doInput = true
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                try {
                    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) error("Gemini API $code: ${safeError(text)}")
                    val json = JSONObject(text)
                    json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.takeIf { it.isNotBlank() }
                        ?: error("Gemini returned no text.")
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
