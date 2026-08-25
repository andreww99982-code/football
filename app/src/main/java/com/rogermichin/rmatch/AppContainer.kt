package com.rogermichin.rmatch

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.rogermichin.rmatch.data.AnalystEngine
import com.rogermichin.rmatch.data.ApiFootballService
import com.rogermichin.rmatch.data.ApiKeyInterceptor
import com.rogermichin.rmatch.data.ApiKeyStore
import com.rogermichin.rmatch.data.CacheDatabase
import com.rogermichin.rmatch.data.FootballRepository
import com.rogermichin.rmatch.data.PreferenceStore
import com.rogermichin.rmatch.data.QuotaTracker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "rmatch_prefs")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val cacheDb = Room.databaseBuilder(
        appContext,
        CacheDatabase::class.java,
        "rmatch_cache.db"
    ).fallbackToDestructiveMigration().build()
    private val quotaTracker = QuotaTracker()
    private val apiKeyStore = ApiKeyStore(appContext)
    private val preferenceStore = PreferenceStore(appContext.dataStore)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(ApiKeyInterceptor(apiKeyStore, quotaTracker))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://v3.football.api-sports.io/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    private val service = retrofit.create(ApiFootballService::class.java)

    val repository = FootballRepository(
        service = service,
        moshi = moshi,
        cacheDao = cacheDb.cacheDao(),
        apiKeyStore = apiKeyStore,
        preferenceStore = preferenceStore,
        quotaTracker = quotaTracker,
        analystEngine = AnalystEngine(),
    )
}
