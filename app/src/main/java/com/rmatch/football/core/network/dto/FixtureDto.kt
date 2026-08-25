package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class FixtureDto(
    val fixture: FixtureCoreDto? = null,
    val league: LeagueRefDto? = null,
    val teams: FixtureTeamsDto? = null,
    val goals: GoalsDto? = null,
    val score: ScoreDto? = null
)

@Serializable
data class FixtureCoreDto(
    val id: Int? = null,
    val referee: String? = null,
    val timezone: String? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val venue: VenueDto? = null,
    val status: FixtureStatusDto? = null
)

@Serializable
data class VenueDto(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null,
    val address: String? = null,
    val capacity: Int? = null,
    val surface: String? = null,
    val image: String? = null
)

@Serializable
data class FixtureStatusDto(
    val long: String? = null,
    val short: String? = null,
    val elapsed: Int? = null
)

@Serializable
data class LeagueRefDto(
    val id: Int? = null,
    val name: String? = null,
    val country: String? = null,
    val logo: String? = null,
    val flag: String? = null,
    val season: Int? = null,
    val round: String? = null
)

@Serializable
data class FixtureTeamsDto(
    val home: TeamRefDto? = null,
    val away: TeamRefDto? = null
)

@Serializable
data class ScoreDto(
    val halftime: GoalsDto? = null,
    val fulltime: GoalsDto? = null,
    val extratime: GoalsDto? = null,
    val penalty: GoalsDto? = null
)

@Serializable
data class FixtureEventDto(
    val time: EventTimeDto? = null,
    val team: TeamRefDto? = null,
    val player: PlayerRefDto? = null,
    val assist: PlayerRefDto? = null,
    val type: String? = null,
    val detail: String? = null,
    val comments: String? = null
)

@Serializable
data class EventTimeDto(
    val elapsed: Int? = null,
    val extra: Int? = null
)
