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

object ApiHub {
    private const val PREFS = "jarvis_api_hub"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jarvis_api_hub_aes"
    private const val KEY_API = "api_key_ciphertext"
    private const val KEY_IV = "api_key_iv"
    const val GEMINI_PROVIDER = "Gemini"
    const val DEFAULT_MODEL = "gemini-3.8-flash"
    private const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    data class Config(val provider: String, val baseUrl: String, val apiKey: String, val model: String)

    fun save(context: Context, config: Config) {
        val key = config.apiKey.trim()
        require(key.isNotBlank()) { "Paste your Gemini API key first." }
        val encrypted = encrypt(key)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("provider", GEMINI_PROVIDER)
            .putString("base_url", DEFAULT_BASE_URL)
            .putString("model", DEFAULT_MODEL)
            .putString(KEY_API, encrypted.first)
            .putString(KEY_IV, encrypted.second)
            .apply()
    }

    /** Accept only a Gemini key; never probes or sends keys to other providers. */
    fun autoConfigure(context: Context, apiKey: String): ApiKeyAutoDetector.Detection? {
        val key = apiKey.trim()
        if (key.isBlank() || !key.startsWith("AIza")) return null
        val detection = ApiKeyAutoDetector.Detection(
            provider = GEMINI_PROVIDER,
            baseUrl = DEFAULT_BASE_URL,
            model = DEFAULT_MODEL,
            message = "Gemini API key format recognized."
        )
        save(context, Config(GEMINI_PROVIDER, DEFAULT_BASE_URL, key, DEFAULT_MODEL))
        return detection
    }

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            GEMINI_PROVIDER,
            DEFAULT_BASE_URL,
            decrypt(prefs.getString(KEY_API, null), prefs.getString(KEY_IV, null)),
            DEFAULT_MODEL
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
        put("provider", GEMINI_PROVIDER)
        put("baseUrl", DEFAULT_BASE_URL)
        put("model", DEFAULT_MODEL)
        put("configured", load(context).apiKey.isNotBlank())
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
            Log.w("ApiHub", "Could not decrypt saved Gemini key; ask user to enter it again.")
            ""
        }
    }
}
