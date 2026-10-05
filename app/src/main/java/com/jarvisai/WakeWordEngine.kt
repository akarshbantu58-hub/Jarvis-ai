package com.jarvisai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Local wake-word architecture.
 *
 * This implementation uses Android's recognition service as a compatibility fallback and
 * looks for "hey jarvis" / "hey jarvis" variants in recognition results. It does not upload
 * audio itself. A dedicated on-device keyword-spotting engine can replace this class later
 * without changing the foreground-service contract.
 */
class WakeWordEngine(
    context: Context,
    private val onWakeWord: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private var running = false
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (running) return
        if (!isAvailable()) {
            onError("No compatible speech-recognition service is available for wake-word mode.")
            return
        }
        running = true
        createRecognizer()
        listen()
    }

    fun stop() {
        running = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
        }
    }

    private fun listen() {
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure { onError("Wake-word recognition could not start: ${it.message ?: "unknown error"}") }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (running) listen()
        }

        override fun onResults(results: Bundle?) {
            val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (phrases.any(::containsWakeWord)) {
                onWakeWord()
            }
            if (running) listen()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val phrases = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (phrases.any(::containsWakeWord)) {
                onWakeWord()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun containsWakeWord(text: String): Boolean {
        val normalized = text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.contains("hey jarvis") ||
            normalized.contains("hey, jarvis") ||
            normalized.contains("a jarvis")
    }
}
