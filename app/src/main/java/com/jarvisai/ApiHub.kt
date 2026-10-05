package com.jarvisai

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ApiHub {
    private const val PREFS = "jarvis_api_hub"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jarvis_api_hub_aes"
    private const val KEY_API = "api_key_ciphertext"
    private const val KEY_IV = "api_key_iv"

    data class Config(val provider: String, val baseUrl: String, val apiKey: String, val model: String)

    fun save(context: Context, config: Config) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("provider", config.provider.trim())
            .putString("base_url", config.baseUrl.trim())
            .putString("model", config.model.trim())
            .remove(KEY_API)
            .remove(KEY_IV)

        if (config.apiKey.isNotBlank()) {
            val encrypted = encrypt(config.apiKey)
            editor.putString(KEY_API, encrypted.first)
                .putString(KEY_IV, encrypted.second)
        }
        editor.apply()
    }

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            prefs.getString("provider", "OpenAI-compatible") ?: "OpenAI-compatible",
            prefs.getString("base_url", "https://api.openai.com/v1/chat/completions") ?: "",
            decrypt(prefs.getString(KEY_API, null), prefs.getString(KEY_IV, null)),
            prefs.getString("model", "gpt-4o-mini") ?: ""
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply {
                load(null)
                if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
            }
        }
    }

    fun exportSafe(context: Context): JSONObject = JSONObject().apply {
        val c = load(context)
        put("provider", c.provider)
        put("baseUrl", c.baseUrl)
        put("model", c.model)
        put("configured", c.apiKey.isNotBlank())
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance("AES", KEYSTORE)
        generator.init(256)
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
        }.getOrDefault("")
    }
}
