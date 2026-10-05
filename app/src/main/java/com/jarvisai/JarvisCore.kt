package com.jarvisai

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import com.jarvisai.voice.TextToSpeechManager
import com.jarvisai.voice.VoiceCommandEngine
import java.util.Locale

/** Core runtime facade for voice output and deterministic command parsing. */
class JarvisCore(private val context: Context) {
    private val actions = AppActionEngine(context)
    private val commandEngine = VoiceCommandEngine()
    private val tts = TextToSpeechManager(context)

    fun speak(text: String): Boolean = tts.speak(text)

    fun stopSpeaking() {
        tts.stop()
    }

    fun isSpeechReady(): Boolean = tts.isReady()

    fun voiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask JARVIS")
    }

    fun parseVoiceCommand(command: String): VoiceCommandEngine.ParsedCommand =
        commandEngine.parse(command)

    fun executeCommand(command: String): String {
        actions.execute(command)?.let { return it }
        val c = command.lowercase(Locale.getDefault()).trim()
        return when {
            c.contains("hello") || c.contains("hi jarvis") -> "Hello. JARVIS is online and ready."
            else -> "I heard: $command. Connect an AI provider to enable intelligent answers."
        }
    }

    fun release() {
        tts.shutdown()
    }
}
