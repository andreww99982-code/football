package com.rmatch.football.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Storage abstraction for the user supplied API-Football key.
 * The key is never bundled with the application, never logged and never committed.
 */
interface ApiKeyStorage {
    fun saveKey(key: String)
    fun getKey(): String?
    fun clearKey()
    fun hasKey(): Boolean
}

/**
 * Production implementation backed by EncryptedSharedPreferences (AES256).
 */
class ApiKeyStore(context: Context) : ApiKeyStorage {

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveKey(key: String) {
        prefs.edit().putString(KEY_API, key.trim()).apply()
    }

    override fun getKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    override fun clearKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    override fun hasKey(): Boolean = getKey() != null

    private companion object {
        const val PREFS_NAME = "rmatch_secure_prefs"
        const val KEY_API = "api_key"
    }
}

/**
 * In-memory storage used by unit tests only.
 */
class InMemoryApiKeyStorage(initial: String? = null) : ApiKeyStorage {
    private var value: String? = initial

    override fun saveKey(key: String) {
        value = key.trim()
    }

    override fun getKey(): String? = value?.takeIf { it.isNotBlank() }

    override fun clearKey() {
        value = null
    }

    override fun hasKey(): Boolean = getKey() != null
}

/**
 * Local (offline) sanity check of the key format. It never replaces the real
 * server side validation performed by [com.rmatch.football.core.data.FootballRepository].
 */
object ApiKeyValidator {

    const val MIN_LENGTH = 20
    const val MAX_LENGTH = 128

    fun isPlausible(raw: String?): Boolean {
        val key = raw?.trim().orEmpty()
        if (key.length < MIN_LENGTH || key.length > MAX_LENGTH) return false
        return key.all { it.isLetterOrDigit() }
    }

    /** Masked representation for the UI: never shows the full secret. */
    fun mask(raw: String?): String {
        val key = raw?.trim().orEmpty()
        if (key.isEmpty()) return "—"
        if (key.length <= 8) return "•".repeat(key.length)
        return key.take(4) + "•".repeat(key.length - 8) + key.takeLast(4)
    }
}
