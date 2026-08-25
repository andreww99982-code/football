package com.rmatch.football.core.network

import com.rmatch.football.core.domain.model.QuotaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps the last quota values reported by the provider response headers. */
class QuotaTracker {

    private val state = MutableStateFlow(QuotaInfo(null, null, null))
    val quota: StateFlow<QuotaInfo> = state.asStateFlow()

    fun update(remaining: Int?, limit: Int?, nowMillis: Long) {
        if (remaining == null && limit == null) return
        state.value = QuotaInfo(
            remaining = remaining ?: state.value.remaining,
            limit = limit ?: state.value.limit,
            updatedAtMillis = nowMillis
        )
    }

    fun reset() {
        state.value = QuotaInfo(null, null, null)
    }
}
