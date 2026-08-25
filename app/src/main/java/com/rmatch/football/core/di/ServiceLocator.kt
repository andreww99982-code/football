package com.rmatch.football.core.di

import android.content.Context
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.datastore.SettingsDataStore
import com.rmatch.football.core.db.RMatchDatabase
import com.rmatch.football.core.domain.usecase.AnalyzeFixtureUseCase
import com.rmatch.football.core.network.FootballApi
import com.rmatch.football.core.network.NetworkModule
import com.rmatch.football.core.network.QuotaTracker
import com.rmatch.football.core.security.ApiKeyStorage
import com.rmatch.football.core.security.ApiKeyStore
import com.rmatch.football.core.util.NetworkMonitor

/** Manual dependency container. Everything is created lazily and lives for the process. */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val apiKeyStorage: ApiKeyStorage by lazy { ApiKeyStore(appContext) }

    val quotaTracker: QuotaTracker by lazy { QuotaTracker() }

    private val database: RMatchDatabase by lazy { RMatchDatabase.build(appContext) }

    private val api: FootballApi by lazy {
        NetworkModule.api(NetworkModule.okHttpClient(apiKeyStorage, quotaTracker))
    }

    val repository: FootballRepository by lazy {
        FootballRepository(
            api = api,
            cacheDao = database.apiCacheDao(),
            json = NetworkModule.json,
            keyStorage = apiKeyStorage,
            quotaTracker = quotaTracker
        )
    }

    val analyzeFixture: AnalyzeFixtureUseCase by lazy { AnalyzeFixtureUseCase(repository) }

    val settings: SettingsDataStore by lazy { SettingsDataStore(appContext) }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }
}
