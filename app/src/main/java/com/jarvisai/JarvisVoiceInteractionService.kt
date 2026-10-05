package com.jarvisai

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService

/**
 * Android-managed default-assistant entry point.
 * The user must explicitly select JARVIS OS as the device assistant.
 */
class JarvisVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    /**
     * Opens the normal JARVIS UI when Android invokes the assistant entry point.
     * Android remains responsible for deciding when this service is invoked.
     */
    fun openJarvis() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
