package com.jarvisai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class JarvisCore(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.92f)
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
    }

    fun voiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask JARVIS")
    }

    fun executeCommand(command: String): String {
        val c = command.lowercase(Locale.getDefault()).trim()
        return when {
            c.contains("open youtube") -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                "Opening YouTube."
            }
            c.contains("open browser") || c.contains("open chrome") -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                "Opening the browser."
            }
            c.contains("battery") -> "Battery information is available from the system panel."
            c.contains("hello") || c.contains("hi jarvis") -> "Hello Akarsh. JARVIS is online and ready."
            else -> "I heard: $command. Connect an AI provider to enable intelligent answers."
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
