package com.jarvisai.voice

import android.content.Context
import android.media.MediaPlayer
import com.jarvisai.ElevenLabsClient
import com.jarvisai.ElevenLabsConfig
import java.io.File

/** Uses ElevenLabs when configured, otherwise the existing local Android TTS manager. */
class HybridSpeechManager(context: Context) {
    private val appContext = context.applicationContext
    private val androidTts = TextToSpeechManager(appContext)
    private var player: MediaPlayer? = null

    fun isReady(): Boolean = ElevenLabsConfig.configured(appContext) || androidTts.isReady()

    fun speak(text: String): Boolean {
        if (text.isBlank()) return false
        if (!ElevenLabsConfig.configured(appContext)) return androidTts.speak(text)
        stop()
        ElevenLabsClient.synthesize(appContext, text) { result ->
            result.onSuccess { bytes ->
                val file = File.createTempFile("jarvis_voice_", ".mp3", appContext.cacheDir)
                file.writeBytes(bytes)
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener { release(); player = null; file.delete() }
                    setOnErrorListener { _, _, _ -> release(); player = null; file.delete(); true }
                    setOnPreparedListener { start() }
                    prepareAsync()
                }
            }.onFailure {
                androidTts.speak(text)
            }
        }
        return true
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        androidTts.stop()
    }

    fun shutdown() { stop(); androidTts.shutdown() }
}
