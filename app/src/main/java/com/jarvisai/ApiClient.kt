package com.jarvisai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object ApiClient {
    fun ask(context: Context, prompt: String, callback: (String) -> Unit) {
        val config = ApiHub.load(context)
        if (config.apiKey.isBlank() || config.baseUrl.isBlank()) { callback("API Hub is not configured. Open API HUB and add your provider and key."); return }
        Thread {
            val result = runCatching {
                val body = JSONObject().apply {
                    put("model", config.model)
                    put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                }.toString()
                val conn = (URL(config.baseUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val text = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) error("API ${conn.responseCode}: $text")
                val json = JSONObject(text)
                json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                    ?: json.optString("output_text", "The provider returned no text.")
            }.getOrElse { "AI request failed: ${it.message ?: "unknown error"}" }
            context.mainExecutor.execute { callback(result) }
        }.start()
    }
}
