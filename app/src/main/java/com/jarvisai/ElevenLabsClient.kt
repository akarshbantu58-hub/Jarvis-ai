package com.jarvisai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ElevenLabsClient {
    fun synthesize(context: Context, text: String, callback: (Result<ByteArray>) -> Unit) {
        if (!ElevenLabsConfig.configured(context)) { callback(Result.failure(IllegalStateException("ElevenLabs is not configured."))); return }
        Thread {
            val result = runCatching {
                val voice = ElevenLabsConfig.voiceId(context)
                val model = ElevenLabsConfig.model(context)
                val endpoint = "https://api.elevenlabs.io/v1/text-to-speech/$voice?output_format=mp3_44100_128"
                val body = JSONObject().apply {
                    put("text", text.take(5000))
                    put("model_id", model)
                    put("voice_settings", JSONObject().put("stability", 0.5).put("similarity_boost", 0.8).put("style", 0.2).put("use_speaker_boost", true))
                }.toString()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 60000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "audio/mpeg")
                    connection.setRequestProperty("xi-api-key", ElevenLabsConfig.apiKey(context))
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        error("ElevenLabs API $code: ${error.take(300)}")
                    }
                    connection.inputStream.use { it.readBytes() }
                } finally { connection.disconnect() }
            }
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }
}
