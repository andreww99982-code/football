package com.rmatch.football.core.network.free

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** TheSportsDB v1 free-tier response envelope for day events. */
@Serializable
data class TheSportsDbEventsEnvelope(
    @SerialName("events") val events: List<TheSportsDbEventDto>? = null
)

@Serializable
data class TheSportsDbEventDto(
    @SerialName("idEvent") val idEvent: String? = null,
    @SerialName("strEvent") val strEvent: String? = null,
    @SerialName("strFilename") val strFilename: String? = null,
    @SerialName("idLeague") val idLeague: String? = null,
    @SerialName("strLeague") val strLeague: String? = null,
    @SerialName("strCountry") val strCountry: String? = null,
    @SerialName("strSport") val strSport: String? = null,
    @SerialName("strHomeTeam") val strHomeTeam: String? = null,
    @SerialName("strAwayTeam") val strAwayTeam: String? = null,
    @SerialName("intHomeScore") val intHomeScore: String? = null,
    @SerialName("intAwayScore") val intAwayScore: String? = null,
    @SerialName("dateEvent") val dateEvent: String? = null,
    @SerialName("strTime") val strTime: String? = null,
    @SerialName("strTimestamp") val strTimestamp: String? = null,
    @SerialName("strStatus") val strStatus: String? = null,
    @SerialName("strThumb") val strThumb: String? = null,
    @SerialName("idHomeTeam") val idHomeTeam: String? = null,
    @SerialName("idAwayTeam") val idAwayTeam: String? = null,
    @SerialName("strHomeTeamBadge") val strHomeTeamBadge: String? = null,
    @SerialName("strAwayTeamBadge") val strAwayTeamBadge: String? = null,
    @SerialName("strVenue") val strVenue: String? = null,
    @SerialName("strCity") val strCity: String? = null
)
