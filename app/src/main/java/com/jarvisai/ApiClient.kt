package com.jarvisai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object ApiClient {
    fun ask(context: Context, prompt: String, callback: (String) -> Unit) {
        val config = ApiHub.load(context)
        if (config.apiKey.isBlank() || config.baseUrl.isBlank()) {
            callback("API Hub is not configured. Open API HUB and add your provider and key.")
            return
        }

        val parsedUrl = runCatching { URL(config.baseUrl) }.getOrNull()
        if (parsedUrl == null || !isAllowedEndpoint(parsedUrl)) {
            callback("API endpoint must use HTTPS. Local HTTP is allowed only for localhost/127.0.0.1.")
            return
        }

        Thread {
            val result = runCatching {
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                }.toString()

                val conn = (parsedUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    useCaches = false
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }

                try {
                    conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                    val responseCode = conn.responseCode
                    val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                    val text = stream?.bufferedReader()?.use { it.readText() } ?: "No response body."
                    if (responseCode !in 200..299) error("API $responseCode: ${safeError(text)}")

                    val json = JSONObject(text)
                    json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                        ?.takeIf { it.isNotBlank() }
                        ?: json.optString("output_text").takeIf { it.isNotBlank() }
                        ?: "The provider returned no text."
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { "AI request failed: ${it.message ?: "unknown error"}" }

            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    private fun isAllowedEndpoint(url: URL): Boolean {
        val protocol = url.protocol.lowercase()
        if (protocol == "https") return true
        if (protocol != "http") return false
        val host = url.host.lowercase()
        return host == "localhost" || host == "127.0.0.1" || host == "::1"
    }

    private fun safeError(body: String): String = body
        .replace(Regex("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*[^,\\s]+"), "$1: [REDACTED]")
        .take(500)
}
