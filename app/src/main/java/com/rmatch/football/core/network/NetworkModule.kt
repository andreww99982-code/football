package com.rmatch.football.core.network

import com.rmatch.football.core.network.free.TheSportsDbApi
import com.rmatch.football.core.security.ApiKeyStorage
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun okHttpClient(
        keyStorage: ApiKeyStorage,
        quotaTracker: QuotaTracker
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(keyStorage))
        .addInterceptor(QuotaInterceptor(quotaTracker))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    fun api(client: OkHttpClient): FootballApi = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FootballApi::class.java)

    /** Free TheSportsDB API (no key required). */
    fun theSportsDbApi(): TheSportsDbApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://www.thesportsdb.com/api/v1/json/3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TheSportsDbApi::class.java)
    }
}
