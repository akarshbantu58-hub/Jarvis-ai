package com.jarvisai

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.jarvisai.ai.AiProviderManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** One-shot Gemini vision request. Frames are never streamed continuously. */
object VisionClient {
    fun describe(context: Context, bitmap: Bitmap, callback: (Result<String>) -> Unit) {
        val config = AiProviderManager(context).configuration()
        if (!config.provider.contains("gemini", ignoreCase = true) || config.apiKey.isBlank()) {
            callback(Result.failure(IllegalStateException("Configure a Gemini API key in API HUB first.")))
            return
        }
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val result = runCatching {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
                val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                val base = config.baseUrl.trimEnd('/').let {
                    if (it.isBlank() || it.contains("api.openai.com")) "https://generativelanguage.googleapis.com/v1beta" else it
                }
                val endpoint = if (base.contains("{model}")) base.replace("{model}", config.model)
                else "$base/models/${config.model}:generateContent"
                val body = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                        .put(JSONObject().put("text", "Describe this image for the user. Be concise and mention important visible details."))
                        .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", encoded)))
                    )))
                }.toString()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.doInput = true
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("x-goog-api-key", config.apiKey)
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val streamIn = if (code in 200..299) connection.inputStream else connection.errorStream
                    val response = streamIn?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) error("Gemini vision API $code: ${response.take(400)}")
                    JSONObject(response).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                        ?.let { parts -> buildString { for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty()) } }
                        ?.takeIf { it.isNotBlank() }
                        ?: error("Gemini returned no vision text.")
                } finally { connection.disconnect() }
            }
            handler.post { callback(result) }
        }.start()
    }
}
