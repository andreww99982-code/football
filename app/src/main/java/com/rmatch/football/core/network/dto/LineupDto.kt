package com.rmatch.football.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LineupDto(
    val team: TeamRefDto? = null,
    val coach: LineupCoachDto? = null,
    val formation: String? = null,
    val startXI: List<LineupSlotDto>? = null,
    val substitutes: List<LineupSlotDto>? = null
)

@Serializable
data class LineupCoachDto(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null
)

@Serializable
data class LineupSlotDto(
    val player: LineupPlayerDto? = null
)

@Serializable
data class LineupPlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val number: Int? = null,
    val pos: String? = null,
    val grid: String? = null
)

@Serializable
data class TeamStatisticsBlockDto(
    val team: TeamRefDto? = null,
    val statistics: List<StatisticEntryDto>? = null
)

@Serializable
data class StatisticEntryDto(
    val type: String? = null,
    val value: JsonElement? = null
)
