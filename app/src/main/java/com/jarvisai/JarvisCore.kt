package com.jarvisai

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class JarvisCore(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ttsReady = false
    private val actions = AppActionEngine(context)

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(0.92f)
        }
    }

    fun speak(text: String): Boolean {
        if (!ttsReady || text.isBlank()) return false
        return tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response") == TextToSpeech.SUCCESS
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun voiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask JARVIS")
    }

    fun executeCommand(command: String): String {
        actions.execute(command)?.let { return it }
        val c = command.lowercase(Locale.getDefault()).trim()
        return when {
            c.contains("battery") -> "Battery information is available from the system panel."
            c.contains("hello") || c.contains("hi jarvis") -> "Hello. JARVIS is online and ready."
            else -> "I heard: $command. Connect an AI provider to enable intelligent answers."
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
