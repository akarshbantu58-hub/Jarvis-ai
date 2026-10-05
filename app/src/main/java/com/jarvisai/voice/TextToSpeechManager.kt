package com.jarvisai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Central Android TTS adapter. It never requires network access and falls back
 * to the device's configured engine/language when the preferred locale is unavailable.
 */
class TextToSpeechManager(
    context: Context,
    private val listener: Listener? = null
) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onReady()
        fun onStarted()
        fun onCompleted()
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                listener?.onStarted()
            }

            override fun onDone(utteranceId: String?) {
                listener?.onCompleted()
            }

            @Deprecated("Deprecated by Android API")
            override fun onError(utteranceId: String?) {
                listener?.onError("Text-to-speech failed.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                listener?.onError("Text-to-speech failed (code $errorCode).")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ready = false
            listener?.onError("Android Text-to-Speech is unavailable.")
            return
        }

        val engine = tts ?: run {
            listener?.onError("Text-to-speech engine was not created.")
            return
        }

        val preferred = engine.setLanguage(Locale.US)
        if (preferred == TextToSpeech.LANG_MISSING_DATA || preferred == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = engine.setLanguage(Locale.getDefault())
            ready = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
            ready = true
        }

        engine.setSpeechRate(0.92f)
        engine.setPitch(1.0f)
        if (ready) listener?.onReady()
    }

    fun isReady(): Boolean = ready

    fun speak(text: String, flush: Boolean = true): Boolean {
        if (!ready || text.isBlank()) return false
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = tts?.speak(text.trim(), queueMode, null, UTTERANCE_ID)
            ?: TextToSpeech.ERROR
        if (result != TextToSpeech.SUCCESS) {
            listener?.onError("Android Text-to-Speech rejected the request.")
            return false
        }
        return true
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    companion object {
        private const val UTTERANCE_ID = "jarvis_response"
    }
}
