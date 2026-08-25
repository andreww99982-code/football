package com.rmatch.football.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Generic API-Football envelope. `response` type varies per endpoint. */
@Serializable
data class ApiEnvelope<T>(
    val get: String? = null,
    val parameters: JsonElement? = null,
    val errors: JsonElement? = null,
    val results: Int = 0,
    val paging: PagingDto? = null,
    val response: T? = null
)

@Serializable
data class PagingDto(
    val current: Int = 0,
    val total: Int = 0
)

@Serializable
data class TeamRefDto(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null,
    val winner: Boolean? = null
)

@Serializable
data class GoalsDto(
    val home: Int? = null,
    val away: Int? = null
)

@Serializable
data class CountryDto(
    val name: String? = null,
    val code: String? = null,
    val flag: String? = null
)

@Serializable
data class PlayerRefDto(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null
)

@Serializable
data class SeasonGoalsDto(
    @SerialName("for") val goalsFor: JsonElement? = null,
    val against: JsonElement? = null
)
