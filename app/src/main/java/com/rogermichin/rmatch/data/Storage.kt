package com.rogermichin.rmatch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface SecureValueStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

class EncryptedPrefsStore(context: Context) : SecureValueStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rmatch_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun read(): String? = prefs.getString(KEY, null)
    override fun write(value: String) { prefs.edit().putString(KEY, value).apply() }
    override fun clear() { prefs.edit().remove(KEY).apply() }

    private companion object { const val KEY = "api_key" }
}

class ApiKeyStore private constructor(
    private val secureValueStore: SecureValueStore,
) {
    constructor(context: Context) : this(EncryptedPrefsStore(context))
    constructor(secureValueStore: SecureValueStore) : this(secureValueStore)

    private val mutableApiKey = MutableStateFlow(secureValueStore.read())
    val apiKeyFlow: StateFlow<String?> = mutableApiKey.asStateFlow()

    fun currentKey(): String? = mutableApiKey.value

    fun save(key: String) {
        secureValueStore.write(key.trim())
        mutableApiKey.value = key.trim()
    }

    fun delete() {
        secureValueStore.clear()
        mutableApiKey.value = null
    }

    fun mask(value: String? = mutableApiKey.value): String = when {
        value.isNullOrBlank() -> "Ключ не задан"
        value.length <= 8 -> "••••${value.takeLast(2)}"
        else -> value.take(3) + "••••••" + value.takeLast(3)
    }
}

class PreferenceStore(private val dataStore: DataStore<Preferences>) {
    private val countryKey = stringPreferencesKey("country_filter")
    private val leagueKey = stringPreferencesKey("league_filter")

    val countryFilter: Flow<String> = dataStore.data.map { it[countryKey].orEmpty() }
    val leagueFilter: Flow<String> = dataStore.data.map { it[leagueKey].orEmpty() }

    suspend fun saveCountryFilter(value: String) { dataStore.edit { it[countryKey] = value } }
    suspend fun saveLeagueFilter(value: String) { dataStore.edit { it[leagueKey] = value } }
}

@Entity(tableName = "payload_cache")
data class CachedPayloadEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val fetchedAt: Long,
    val ttlMillis: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM payload_cache WHERE cacheKey = :key")
    suspend fun get(key: String): CachedPayloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPayloadEntity)

    @Query("DELETE FROM payload_cache")
    suspend fun clearAll()
}

@Database(entities = [CachedPayloadEntity::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
