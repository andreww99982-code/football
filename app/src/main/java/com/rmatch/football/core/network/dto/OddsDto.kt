package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class OddsDto(
    val league: LeagueRefDto? = null,
    val fixture: FixtureCoreDto? = null,
    val update: String? = null,
    val bookmakers: List<BookmakerDto>? = null
)

@Serializable
data class BookmakerDto(
    val id: Int? = null,
    val name: String? = null,
    val bets: List<BetDto>? = null
)

@Serializable
data class BetDto(
    val id: Int? = null,
    val name: String? = null,
    val values: List<BetValueDto>? = null
)

@Serializable
data class BetValueDto(
    val value: String? = null,
    val odd: String? = null
)
