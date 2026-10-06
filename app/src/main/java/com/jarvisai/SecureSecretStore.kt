package com.jarvisai

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureSecretStore {
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "jarvis_secrets_aes"
    private const val PREFS = "jarvis_secrets"

    fun put(context: Context, name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("$name.data", Base64.encodeToString(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP))
            .putString("$name.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun get(context: Context, name: String): String = runCatching {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val data = prefs.getString("$name.data", null) ?: return ""
        val iv = prefs.getString("$name.iv", null) ?: return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }.getOrDefault("")

    fun clear(context: Context, name: String) { context.getSharedPreferences(PREFS, 0).edit().remove("$name.data").remove("$name.iv").apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KS).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", KS).apply { init(256) }.generateKey()
    }
}
