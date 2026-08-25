package com.rmatch.football.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rmatchDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rmatch_settings"
)

/** Non secret user preferences. Secrets live in EncryptedSharedPreferences only. */
class SettingsDataStore(context: Context) {

    private val store = context.applicationContext.rmatchDataStore

    val onboardingCompleted: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING] ?: false }

    val seasonOverride: Flow<Int?> = store.data.map { it[KEY_SEASON] }

    val lastSyncMillis: Flow<Long?> = store.data.map { it[KEY_LAST_SYNC] }

    suspend fun setOnboardingCompleted(value: Boolean) {
        store.edit { it[KEY_ONBOARDING] = value }
    }

    suspend fun setSeasonOverride(season: Int?) {
        store.edit { prefs ->
            if (season == null) prefs.remove(KEY_SEASON) else prefs[KEY_SEASON] = season
        }
    }

    suspend fun setLastSync(millis: Long) {
        store.edit { it[KEY_LAST_SYNC] = millis }
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_SEASON = intPreferencesKey("season_override")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync_millis")
    }
}
