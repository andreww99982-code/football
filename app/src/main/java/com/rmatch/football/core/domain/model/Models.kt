package com.rmatch.football.core.domain.model

/** Domain models. DTOs are never exposed above the mapper layer. */

data class TeamRef(
    val id: Int,
    val name: String,
    val logoUrl: String? = null
)

data class LeagueRef(
    val id: Int,
    val name: String,
    val country: String? = null,
    val logoUrl: String? = null,
    val flagUrl: String? = null,
    val season: Int? = null,
    val round: String? = null
)

data class Venue(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null,
    val capacity: Int? = null,
    val surface: String? = null
)

data class MatchStatus(
    val shortCode: String,
    val description: String,
    val elapsedMinutes: Int? = null
) {
    val isLive: Boolean get() = shortCode in LIVE_CODES
    val isFinished: Boolean get() = shortCode in FINISHED_CODES
    val isUpcoming: Boolean get() = shortCode in UPCOMING_CODES

    companion object {
        val LIVE_CODES = setOf("1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE")
        val FINISHED_CODES = setOf("FT", "AET", "PEN")
        val UPCOMING_CODES = setOf("TBD", "NS")
        val UNKNOWN = MatchStatus("NS", "Не начался", null)
    }
}

data class Fixture(
    val id: Int,
    val timestampSeconds: Long?,
    val dateIso: String?,
    val status: MatchStatus,
    val league: LeagueRef?,
    val home: TeamRef,
    val away: TeamRef,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val halftimeHome: Int? = null,
    val halftimeAway: Int? = null,
    val venue: Venue? = null,
    val referee: String? = null
) {
    val hasScore: Boolean get() = homeGoals != null && awayGoals != null
    val scoreLabel: String get() = if (hasScore) "$homeGoals : $awayGoals" else "—"
}

data class StandingRow(
    val rank: Int,
    val team: TeamRef,
    val points: Int,
    val goalsDiff: Int,
    val group: String?,
    val form: String?,
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val description: String?
)

data class LeagueSummary(
    val league: LeagueRef,
    val countryName: String?,
    val countryFlagUrl: String?,
    val seasons: List<Int>,
    val currentSeason: Int?
)

data class Country(
    val name: String,
    val code: String?,
    val flagUrl: String?
)

data class LineupMan(
    val id: Int?,
    val name: String,
    val number: Int?,
    val position: String?,
    val grid: String?
)

data class Lineup(
    val team: TeamRef?,
    val coachName: String?,
    val formation: String?,
    val startXI: List<LineupMan>,
    val substitutes: List<LineupMan>
)

data class StatEntry(
    val type: String,
    val value: String
)

data class TeamMatchStatistics(
    val team: TeamRef?,
    val entries: List<StatEntry>
)

data class MatchEvent(
    val minute: Int?,
    val extraMinute: Int?,
    val teamName: String?,
    val playerName: String?,
    val assistName: String?,
    val type: String?,
    val detail: String?
)

data class TeamProfile(
    val team: TeamRef,
    val country: String?,
    val founded: Int?,
    val national: Boolean,
    val venue: Venue?
)

data class TeamSeasonStats(
    val form: String?,
    val played: Int?,
    val wins: Int?,
    val draws: Int?,
    val loses: Int?,
    val goalsFor: Int?,
    val goalsAgainst: Int?
)

data class SquadMember(
    val id: Int?,
    val name: String,
    val age: Int?,
    val number: Int?,
    val position: String?,
    val photoUrl: String?
)

data class Coach(
    val id: Int?,
    val name: String,
    val age: Int?,
    val nationality: String?,
    val photoUrl: String?
)

data class Injury(
    val playerId: Int?,
    val playerName: String,
    val teamId: Int?,
    val teamName: String?,
    val type: String?,
    val reason: String?
)

data class PlayerSeasonStats(
    val teamName: String?,
    val leagueName: String?,
    val appearances: Int?,
    val lineups: Int?,
    val minutes: Int?,
    val position: String?,
    val rating: String?,
    val goals: Int?,
    val assists: Int?,
    val yellowCards: Int?,
    val redCards: Int?
)

data class PlayerProfile(
    val id: Int,
    val name: String,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val birthDate: String?,
    val birthPlace: String?,
    val nationality: String?,
    val height: String?,
    val weight: String?,
    val injured: Boolean?,
    val photoUrl: String?,
    val seasons: List<PlayerSeasonStats>
)

data class OddsSelection(
    val label: String,
    val rawOdd: String,
    val decimalOdds: Double?
)

data class OddsMarket(
    val id: Int?,
    val name: String,
    val selections: List<OddsSelection>
)

data class OddsBookmaker(
    val id: Int?,
    val name: String,
    val markets: List<OddsMarket>
)

data class OddsBoard(
    val fixtureId: Int?,
    val updateIso: String?,
    val bookmakers: List<OddsBookmaker>
)

data class QuotaInfo(
    val remaining: Int?,
    val limit: Int?,
    val updatedAtMillis: Long?
)

data class ProviderStatus(
    val plan: String?,
    val subscriptionEnd: String?,
    val active: Boolean?,
    val requestsToday: Int?,
    val requestsLimitPerDay: Int?
)
