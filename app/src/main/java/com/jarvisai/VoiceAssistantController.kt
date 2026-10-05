package com.jarvisai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Foreground, user-initiated speech controller.
 * It does not start microphone capture by itself and never uploads audio.
 */
class VoiceAssistantController(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onListeningChanged(listening: Boolean)
        fun onPartialText(text: String)
        fun onFinalText(text: String)
        fun onAudioLevel(level: Float)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recognizer: SpeechRecognizer? = if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
        SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(recognitionListener) }
    } else {
        null
    }
    private var continuous = false
    private var stopping = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener.onListeningChanged(true)
        }

        override fun onBeginningOfSpeech() {
            listener.onListeningChanged(true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            listener.onAudioLevel(((rmsdB - 1f) / 9f).coerceIn(0f, 1f))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            listener.onListeningChanged(false)
        }

        override fun onError(error: Int) {
            listener.onListeningChanged(false)
            if (!stopping) {
                listener.onError(errorMessage(error))
                if (continuous) scheduleRestart()
            }
        }

        override fun onResults(results: Bundle?) {
            listener.onListeningChanged(false)
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) listener.onFinalText(text)
            if (continuous && !stopping) scheduleRestart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(listener::onPartialText)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun isAvailable(): Boolean = recognizer != null

    fun start(continuousMode: Boolean = false, preferOffline: Boolean = false) {
        val speech = recognizer ?: run {
            listener.onError("No Android speech-recognition service is available.")
            return
        }
        stopping = false
        continuous = continuousMode
        speech.cancel()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask JARVIS")
        }
        runCatching { speech.startListening(intent) }
            .onFailure { listener.onError("Unable to start voice input: ${it.message ?: "unknown error"}") }
    }

    fun stop() {
        stopping = true
        continuous = false
        mainHandler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        listener.onListeningChanged(false)
    }

    fun release() {
        stop()
        recognizer?.destroy()
    }

    private fun scheduleRestart() {
        mainHandler.postDelayed({
            if (!stopping && continuous) start(continuousMode = true)
        }, 350L)
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio could not be read."
        SpeechRecognizer.ERROR_CLIENT -> "Voice input was interrupted."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition service failed."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        else -> "Voice input failed (code $code)."
    }
}
