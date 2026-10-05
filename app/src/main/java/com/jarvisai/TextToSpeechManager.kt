package com.jarvisai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Small replaceable TTS boundary; cloud TTS can be added without changing callers. */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.getDefault()
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-${System.currentTimeMillis()}")
    }

    fun stop() = tts.stop()
    fun release() = tts.shutdown()
}
