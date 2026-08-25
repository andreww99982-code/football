package com.rmatch.football.core.network

import com.rmatch.football.core.security.ApiKeyStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects the user supplied key. The key lives only in EncryptedSharedPreferences,
 * it is never logged and never written to BuildConfig or to the cache database.
 */
class ApiKeyInterceptor(private val keyStorage: ApiKeyStorage) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(ApiConstants.HEADER_KEY) != null) {
            return chain.proceed(original)
        }
        val key = keyStorage.getKey() ?: return chain.proceed(original)
        val request = original.newBuilder()
            .addHeader(ApiConstants.HEADER_KEY, key)
            .build()
        return chain.proceed(request)
    }
}

/** Reads the provider quota headers so Settings can display the remaining requests. */
class QuotaInterceptor(
    private val quotaTracker: QuotaTracker,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val remaining = response.header(ApiConstants.HEADER_QUOTA_REMAINING)?.toIntOrNull()
        val limit = response.header(ApiConstants.HEADER_QUOTA_LIMIT)?.toIntOrNull()
        quotaTracker.update(remaining, limit, nowProvider())
        return response
    }
}
