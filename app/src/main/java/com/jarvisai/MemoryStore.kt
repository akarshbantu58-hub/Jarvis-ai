package com.jarvisai

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local encrypted conversation/preferences store. Nothing is uploaded automatically. */
object MemoryStore {
    private const val PREFS = "jarvis_memory"
    private const val KEY_DATA = "data"
    private const val KEY_IV = "iv"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "jarvis_memory_aes"

    data class Entry(val role: String, val text: String, val time: Long)

    fun addConversation(context: Context, role: String, text: String) {
        if (text.isBlank()) return
        val list = conversations(context).toMutableList()
        list += Entry(role.take(24), text.take(10000), System.currentTimeMillis())
        saveConversations(context, list.takeLast(100))
    }

    fun conversations(context: Context): List<Entry> = runCatching {
        val json = decrypt(context) ?: return emptyList()
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(Entry(item.optString("role"), item.optString("text"), item.optLong("time")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveUserMemory(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("memory_${key.trim().take(80)}", value.take(4000)).apply()
    }

    fun userMemories(context: Context): Map<String, String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .all.filterKeys { it.startsWith("memory_") }
        .mapKeys { it.key.removePrefix("memory_") }
        .mapValues { it.value?.toString().orEmpty() }

    fun deleteUserMemory(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("memory_$key").apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply {
                load(null)
                if (containsAlias(ALIAS)) deleteEntry(ALIAS)
            }
        }
    }

    private fun saveConversations(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().apply {
            put("role", it.role)
            put("text", it.text)
            put("time", it.time)
        }) }
        encrypt(context, array.toString())
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", KEYSTORE).apply { init(256) }.generateKey()
    }

    private fun encrypt(context: Context, plain: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DATA, Base64.encodeToString(cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun decrypt(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_DATA, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }
}
