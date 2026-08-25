package com.rogermichin.rmatch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response

class QuotaTracker {
    private val mutableQuota = MutableStateFlow(QuotaInfo())
    val quota = mutableQuota.asStateFlow()

    fun updateFromHeaders(headers: Headers) {
        mutableQuota.value = QuotaInfo(
            requestsLimit = headers["x-ratelimit-requests-limit"]?.toIntOrNull(),
            requestsRemaining = headers["x-ratelimit-requests-remaining"]?.toIntOrNull(),
            dailyLimit = headers["x-ratelimit-limit-day"]?.toIntOrNull(),
            usedToday = headers["x-ratelimit-current-day"]?.toIntOrNull(),
        )
    }
}

class ApiKeyInterceptor(
    private val apiKeyStore: ApiKeyStore,
    private val quotaTracker: QuotaTracker,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        apiKeyStore.currentKey()?.takeIf { it.isNotBlank() }?.let { builder.header("x-apisports-key", it) }
        val response = chain.proceed(builder.build())
        quotaTracker.updateFromHeaders(response.headers)
        return response
    }
}
