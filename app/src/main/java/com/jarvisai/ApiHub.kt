package com.jarvisai

import android.content.Context
import org.json.JSONObject

object ApiHub {
    private const val PREFS = "jarvis_api_hub"
    data class Config(val provider: String, val baseUrl: String, val apiKey: String, val model: String)
    fun save(context: Context, config: Config) = context.getSharedPreferences(PREFS, 0).edit()
        .putString("provider", config.provider).putString("base_url", config.baseUrl)
        .putString("api_key", config.apiKey).putString("model", config.model).apply()
    fun load(context: Context): Config {
        val p = context.getSharedPreferences(PREFS, 0)
        return Config(p.getString("provider", "OpenAI-compatible") ?: "OpenAI-compatible",
            p.getString("base_url", "https://api.openai.com/v1/chat/completions") ?: "",
            p.getString("api_key", "") ?: "", p.getString("model", "gpt-4o-mini") ?: "")
    }
    fun clear(context: Context) = context.getSharedPreferences(PREFS, 0).edit().clear().apply()
    fun exportSafe(context: Context): JSONObject = JSONObject().apply {
        val c = load(context); put("provider", c.provider); put("baseUrl", c.baseUrl)
        put("model", c.model); put("configured", c.apiKey.isNotBlank())
    }
}
