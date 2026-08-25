package com.rmatch.football.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val account: StatusAccountDto? = null,
    val subscription: StatusSubscriptionDto? = null,
    val requests: StatusRequestsDto? = null
)

@Serializable
data class StatusAccountDto(
    val firstname: String? = null,
    val lastname: String? = null
)

@Serializable
data class StatusSubscriptionDto(
    val plan: String? = null,
    val end: String? = null,
    val active: Boolean? = null
)

@Serializable
data class StatusRequestsDto(
    val current: Int? = null,
    @SerialName("limit_day") val limitDay: Int? = null
)
