package com.jarvisai

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisVoiceSession(this)
}

class JarvisVoiceSession(service: JarvisSessionService) : VoiceInteractionSession(service) {
    override fun onHandleAssist(data: android.app.assist.AssistStructure?, content: Bundle?) {
        // Android-managed assistant context is available here for approved integrations.
        super.onHandleAssist(data, content)
    }
}
