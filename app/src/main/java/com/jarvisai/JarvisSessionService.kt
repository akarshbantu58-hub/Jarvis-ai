package com.jarvisai

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisVoiceSession(this)
}

class JarvisVoiceSession(service: JarvisSessionService) : VoiceInteractionSession(service) {
    @Suppress("DEPRECATION")
    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        // Android supplies assistant context through this approved callback.
        super.onHandleAssist(data, structure, content)
    }
}
