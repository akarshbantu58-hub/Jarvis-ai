package com.jarvisai.ai

import com.jarvisai.ApiHub

interface AiProvider {
    val id: String
    val displayName: String

    fun isConfigured(config: ApiHub.Config): Boolean

    fun ask(
        config: ApiHub.Config,
        prompt: String,
        callback: (Result<String>) -> Unit
    )
}
