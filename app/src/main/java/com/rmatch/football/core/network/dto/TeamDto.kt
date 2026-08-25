package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamProfileDto(
    val team: TeamCoreDto? = null,
    val venue: VenueDto? = null
)

@Serializable
data class TeamCoreDto(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val country: String? = null,
    val founded: Int? = null,
    val national: Boolean? = null,
    val logo: String? = null
)

@Serializable
data class TeamStatisticsDto(
    val league: LeagueRefDto? = null,
    val team: TeamRefDto? = null,
    val form: String? = null,
    val fixtures: TeamFixturesDto? = null,
    val goals: TeamGoalsDto? = null
)

@Serializable
data class TeamFixturesDto(
    val played: HomeAwayTotalDto? = null,
    val wins: HomeAwayTotalDto? = null,
    val draws: HomeAwayTotalDto? = null,
    val loses: HomeAwayTotalDto? = null
)

@Serializable
data class HomeAwayTotalDto(
    val home: Int? = null,
    val away: Int? = null,
    val total: Int? = null
)

@Serializable
data class TeamGoalsDto(
    @kotlinx.serialization.SerialName("for") val goalsFor: TeamGoalsDetailDto? = null,
    val against: TeamGoalsDetailDto? = null
)

@Serializable
data class TeamGoalsDetailDto(
    val total: HomeAwayTotalDto? = null
)

@Serializable
data class SquadDto(
    val team: TeamRefDto? = null,
    val players: List<SquadPlayerDto>? = null
)

@Serializable
data class SquadPlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null,
    val number: Int? = null,
    val position: String? = null,
    val photo: String? = null
)

@Serializable
data class CoachDto(
    val id: Int? = null,
    val name: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val age: Int? = null,
    val nationality: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val photo: String? = null,
    val team: TeamRefDto? = null
)
