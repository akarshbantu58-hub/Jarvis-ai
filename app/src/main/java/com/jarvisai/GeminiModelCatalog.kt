package com.jarvisai

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches models actually exposed to this API key by Google's Gemini API. */
object GeminiModelCatalog {
    fun fetch(apiKey: String, callback: (Result<List<Model>>) -> Unit) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            callback(Result.failure(IllegalStateException("Save a Gemini API key first.")))
            return
        }
        val main = Handler(Looper.getMainLooper())
        Thread {
            val result = runCatching {
                val connection = URL("https://generativelanguage.googleapis.com/v1beta/models")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("x-goog-api-key", key)
                try {
                    val code = connection.responseCode
                    val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) error("Gemini model list failed ($code). Check the API key and project access.")
                    val models = mutableListOf<Model>()
                    val array = JSONObject(body).optJSONArray("models")
                        ?: error("Google returned no model list.")
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val methods = item.optJSONArray("supportedGenerationMethods") ?: continue
                        if ((0 until methods.length()).none { methods.optString(it) == "generateContent" }) continue
                        val fullName = item.optString("name").removePrefix("models/")
                        if (!fullName.startsWith("gemini-")) continue
                        val display = item.optString("displayName").ifBlank { fullName }
                        val description = item.optString("description")
                        models += Model(fullName, display, description)
                    }
                    models.distinctBy { it.id }.sortedWith(
                        compareBy<Model> { !it.id.contains("flash", ignoreCase = true) }
                            .thenBy { it.displayName.lowercase() }
                    ).also { if (it.isEmpty()) error("No Gemini models supporting generateContent were returned for this key.") }
                } finally {
                    connection.disconnect()
                }
            }
            main.post { callback(result) }
        }.start()
    }

    data class Model(val id: String, val displayName: String, val description: String)
}
