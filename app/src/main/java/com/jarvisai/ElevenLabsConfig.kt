package com.jarvisai

import android.content.Context

object ElevenLabsConfig {
    private const val PREFS = "jarvis_elevenlabs"
    private const val VOICE = "voice"
    private const val MODEL = "model"

    fun save(context: Context, apiKey: String, voiceId: String, model: String) {
        if (apiKey.isBlank()) SecureSecretStore.clear(context, "elevenlabs_key") else SecureSecretStore.put(context, "elevenlabs_key", apiKey.trim())
        context.getSharedPreferences(PREFS, 0).edit().putString(VOICE, voiceId.trim()).putString(MODEL, model.trim().ifBlank { "eleven_multilingual_v2" }).apply()
    }

    fun apiKey(context: Context) = SecureSecretStore.get(context, "elevenlabs_key")
    fun voiceId(context: Context) = context.getSharedPreferences(PREFS, 0).getString(VOICE, "") ?: ""
    fun model(context: Context) = context.getSharedPreferences(PREFS, 0).getString(MODEL, "eleven_multilingual_v2") ?: "eleven_multilingual_v2"
    fun configured(context: Context) = apiKey(context).isNotBlank() && voiceId(context).isNotBlank()
}
