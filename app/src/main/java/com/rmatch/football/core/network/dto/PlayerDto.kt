package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponseDto(
    val player: PlayerCoreDto? = null,
    val statistics: List<PlayerStatisticsDto>? = null
)

@Serializable
data class PlayerCoreDto(
    val id: Int? = null,
    val name: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val age: Int? = null,
    val birth: PlayerBirthDto? = null,
    val nationality: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val injured: Boolean? = null,
    val photo: String? = null
)

@Serializable
data class PlayerBirthDto(
    val date: String? = null,
    val place: String? = null,
    val country: String? = null
)

@Serializable
data class PlayerStatisticsDto(
    val team: TeamRefDto? = null,
    val league: LeagueRefDto? = null,
    val games: PlayerGamesDto? = null,
    val goals: PlayerGoalsDto? = null,
    val cards: PlayerCardsDto? = null
)

@Serializable
data class PlayerGamesDto(
    val appearences: Int? = null,
    val lineups: Int? = null,
    val minutes: Int? = null,
    val number: Int? = null,
    val position: String? = null,
    val rating: String? = null,
    val captain: Boolean? = null
)

@Serializable
data class PlayerGoalsDto(
    val total: Int? = null,
    val conceded: Int? = null,
    val assists: Int? = null,
    val saves: Int? = null
)

@Serializable
data class PlayerCardsDto(
    val yellow: Int? = null,
    val yellowred: Int? = null,
    val red: Int? = null
)

@Serializable
data class InjuryDto(
    val player: InjuryPlayerDto? = null,
    val team: TeamRefDto? = null,
    val fixture: FixtureCoreDto? = null,
    val league: LeagueRefDto? = null
)

@Serializable
data class InjuryPlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null,
    val type: String? = null,
    val reason: String? = null
)
