package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LeagueDto(
    val league: LeagueCoreDto? = null,
    val country: CountryDto? = null,
    val seasons: List<SeasonDto>? = null
)

@Serializable
data class LeagueCoreDto(
    val id: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val logo: String? = null
)

@Serializable
data class SeasonDto(
    val year: Int? = null,
    val start: String? = null,
    val end: String? = null,
    val current: Boolean? = null
)

@Serializable
data class StandingsResponseDto(
    val league: StandingsLeagueDto? = null
)

@Serializable
data class StandingsLeagueDto(
    val id: Int? = null,
    val name: String? = null,
    val country: String? = null,
    val logo: String? = null,
    val flag: String? = null,
    val season: Int? = null,
    val standings: List<List<StandingRowDto>>? = null
)

@Serializable
data class StandingRowDto(
    val rank: Int? = null,
    val team: TeamRefDto? = null,
    val points: Int? = null,
    val goalsDiff: Int? = null,
    val group: String? = null,
    val form: String? = null,
    val status: String? = null,
    val description: String? = null,
    val all: StandingStatsDto? = null,
    val home: StandingStatsDto? = null,
    val away: StandingStatsDto? = null,
    val update: String? = null
)

@Serializable
data class StandingStatsDto(
    val played: Int? = null,
    val win: Int? = null,
    val draw: Int? = null,
    val lose: Int? = null,
    val goals: StandingGoalsDto? = null
)

@Serializable
data class StandingGoalsDto(
    @kotlinx.serialization.SerialName("for") val goalsFor: Int? = null,
    val against: Int? = null
)
