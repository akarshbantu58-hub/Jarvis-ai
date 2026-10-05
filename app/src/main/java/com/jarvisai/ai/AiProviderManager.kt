package com.jarvisai.ai

import android.content.Context
import com.jarvisai.ApiHub

/**
 * Chooses a configured provider without ever discovering or retrieving secrets.
 * Credentials remain owned by ApiHub's encrypted storage.
 */
class AiProviderManager(private val context: Context) {
    private val config: ApiHub.Config
        get() = ApiHub.load(context)

    fun isConfigured(): Boolean = config.apiKey.isNotBlank() && config.baseUrl.isNotBlank()

    fun configuredProviderName(): String? =
        config.takeIf { isConfigured() }?.provider

    fun configuration(): ApiHub.Config = config
}
