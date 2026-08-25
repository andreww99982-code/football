package com.rmatch.football.core.domain.analyst

import com.rmatch.football.core.domain.model.TeamRef

/** One completed match of a team, used as model input. */
data class TeamMatchSample(
    val fixtureId: Int,
    val kickoffSeconds: Long?,
    val isHome: Boolean,
    val goalsScored: Int,
    val goalsConceded: Int
) {
    val points: Int
        get() = when {
            goalsScored > goalsConceded -> 3
            goalsScored == goalsConceded -> 1
            else -> 0
        }

    val resultLetter: String
        get() = when {
            goalsScored > goalsConceded -> "В"
            goalsScored == goalsConceded -> "Н"
            else -> "П"
        }
}

/** Everything known about one side of the tie. Ordered from the most recent match. */
data class TeamSample(
    val team: TeamRef,
    val matches: List<TeamMatchSample>,
    val standingRank: Int? = null,
    val standingPoints: Int? = null,
    val confirmedInjuries: Int = 0,
    val lineupConfirmed: Boolean = false
)

data class AnalystInput(
    val fixtureId: Int,
    val homeSample: TeamSample,
    val awaySample: TeamSample
)

data class OutcomeProbabilities(
    val homeWin: Double,
    val draw: Double,
    val awayWin: Double
)

data class TotalLine(
    val line: Double,
    val over: Double,
    val under: Double
)

data class HandicapLine(
    val line: Double,
    val homeCovers: Double,
    val push: Double,
    val awayCovers: Double
)

data class DataQuality(
    val score: Int,
    val notes: List<String>
)

data class AnalystReport(
    val fixtureId: Int,
    val outcome: OutcomeProbabilities,
    val expectedGoalsHome: Double,
    val expectedGoalsAway: Double,
    val expectedTotalGoals: Double,
    val totals: List<TotalLine>,
    val bttsYes: Double,
    val bttsNo: Double,
    val handicaps: List<HandicapLine>,
    val matchesUsedHome: Int,
    val matchesUsedAway: Int,
    val factors: List<String>,
    val risks: List<String>,
    val dataQuality: DataQuality,
    val computedAtMillis: Long,
    val computationMillis: Long,
    val methodology: String
)

sealed interface AnalystResult {
    data class Ready(val report: AnalystReport) : AnalystResult
    data class Insufficient(val reason: String) : AnalystResult
}
