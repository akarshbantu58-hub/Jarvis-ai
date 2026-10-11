package com.jarvisai

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores a separate encrypted API key and configuration for each provider.
 * Keys stay in Android Keystore-backed AES/GCM encrypted SharedPreferences.
 */
object ApiHub {
    private const val PREFS = "jarvis_api_hub"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jarvis_api_hub_aes"
    private const val LEGACY_KEY_API = "api_key_ciphertext"
    private const val LEGACY_KEY_IV = "api_key_iv"
    private const val SELECTED_PROVIDER = "selected_provider"

    const val GEMINI_PROVIDER = "Gemini"
    const val OPENROUTER_PROVIDER = "OpenRouter"
    const val GROQ_PROVIDER = "Groq"
    const val OPENAI_PROVIDER = "OpenAI-compatible"

    const val DEFAULT_MODEL = "gemini-3.8-flash"
    private const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    data class Config(val provider: String, val baseUrl: String, val apiKey: String, val model: String)

    fun providers(): List<String> = listOf(
        GEMINI_PROVIDER, OPENROUTER_PROVIDER, GROQ_PROVIDER, OPENAI_PROVIDER
    )

    fun defaultConfig(provider: String): Config = when (provider) {
        GEMINI_PROVIDER -> Config(
            GEMINI_PROVIDER, DEFAULT_BASE_URL, "", DEFAULT_MODEL
        )
        OPENROUTER_PROVIDER -> Config(
            OPENROUTER_PROVIDER,
            "https://openrouter.ai/api/v1/chat/completions",
            "",
            "openai/gpt-4o-mini"
        )
        GROQ_PROVIDER -> Config(
            GROQ_PROVIDER,
            "https://api.groq.com/openai/v1/chat/completions",
            "",
            "llama-3.3-70b-versatile"
        )
        else -> Config(
            OPENAI_PROVIDER,
            "https://api.openai.com/v1/chat/completions",
            "",
            "gpt-4o-mini"
        )
    }

    fun save(context: Context, config: Config) {
        val provider = canonicalProvider(config.provider)
        val defaults = defaultConfig(provider)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = providerId(provider)
        val cleanBase = config.baseUrl.trim()
        val cleanModel = config.model.trim()
        require(cleanBase.isNotBlank()) { "Enter the API endpoint URL." }
        require(cleanModel.isNotBlank()) { "Enter the model ID." }
        val oldKey = decrypt(
            prefs.getString("key_${id}_ciphertext", null),
            prefs.getString("key_${id}_iv", null)
        ).ifBlank {
            if (provider == GEMINI_PROVIDER) {
                decrypt(prefs.getString(LEGACY_KEY_API, null), prefs.getString(LEGACY_KEY_IV, null))
            } else ""
        }
        val key = config.apiKey.trim().ifBlank { oldKey }
        val editor = prefs.edit()
            .putString(SELECTED_PROVIDER, provider)
            .putString("base_url_$id", cleanBase)
            .putString("model_$id", cleanModel)
        if (key.isNotBlank()) {
            val encrypted = encrypt(key)
            editor.putString("key_${id}_ciphertext", encrypted.first)
                .putString("key_${id}_iv", encrypted.second)
        }
        // Retain per-provider credentials if the key field is intentionally left blank.
        editor.apply()
    }

    /** Accepts a Gemini key without probing or transmitting it to any other provider. */
    fun autoConfigure(context: Context, apiKey: String): ApiKeyAutoDetector.Detection? {
        val key = apiKey.trim()
        if (key.isBlank() || !key.startsWith("AIza")) return null
        val defaults = defaultConfig(GEMINI_PROVIDER)
        val detection = ApiKeyAutoDetector.Detection(
            provider = GEMINI_PROVIDER,
            baseUrl = defaults.baseUrl,
            model = defaults.model,
            message = "Gemini API key format recognized."
        )
        save(context, defaults.copy(apiKey = key))
        return detection
    }

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val selected = canonicalProvider(prefs.getString(SELECTED_PROVIDER, GEMINI_PROVIDER).orEmpty())
        return load(context, selected)
    }

    fun load(context: Context, providerName: String): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val provider = canonicalProvider(providerName)
        val defaults = defaultConfig(provider)
        val id = providerId(provider)
        val key = decrypt(
            prefs.getString("key_${id}_ciphertext", null),
            prefs.getString("key_${id}_iv", null)
        ).ifBlank {
            if (provider == GEMINI_PROVIDER) {
                decrypt(prefs.getString(LEGACY_KEY_API, null), prefs.getString(LEGACY_KEY_IV, null))
            } else ""
        }
        return Config(
            provider = provider,
            baseUrl = prefs.getString("base_url_$id", null) ?: defaults.baseUrl,
            apiKey = key,
            model = prefs.getString("model_$id", null) ?: defaults.model
        )
    }

    fun clear(context: Context, providerName: String = load(context).provider) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = providerId(canonicalProvider(providerName))
        prefs.edit()
            .remove("key_${id}_ciphertext")
            .remove("key_${id}_iv")
            .remove(LEGACY_KEY_API.takeIf { id == "gemini" } .orEmpty().takeIf { it.isNotBlank() })
            .remove(LEGACY_KEY_IV.takeIf { id == "gemini" }.orEmpty().takeIf { it.isNotBlank() })
            .apply()
        // The key is shared by provider records; deleting it would make other saved keys unreadable.
    }

    fun exportSafe(context: Context): JSONObject {
        val config = load(context)
        return JSONObject().apply {
            put("provider", config.provider)
            put("baseUrl", config.baseUrl)
            put("model", config.model)
            put("configured", config.apiKey.isNotBlank())
        }
    }

    private fun canonicalProvider(value: String): String = when (value.trim().lowercase()) {
        "gemini" -> GEMINI_PROVIDER
        "openrouter" -> OPENROUTER_PROVIDER
        "groq" -> GROQ_PROVIDER
        "openai", "openai-compatible", "custom", "local" -> OPENAI_PROVIDER
        else -> OPENAI_PROVIDER
    }

    private fun providerId(provider: String): String = when (canonicalProvider(provider)) {
        GEMINI_PROVIDER -> "gemini"
        OPENROUTER_PROVIDER -> "openrouter"
        GROQ_PROVIDER -> "groq"
        else -> "openai"
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(ciphertext: String?, iv: String?): String {
        if (ciphertext.isNullOrBlank() || iv.isNullOrBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8)
        }.getOrElse {
            Log.w("ApiHub", "Could not decrypt saved API key; ask user to enter it again.")
            ""
        }
    }
}
