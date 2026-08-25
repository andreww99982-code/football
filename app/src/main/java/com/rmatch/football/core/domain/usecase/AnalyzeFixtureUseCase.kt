package com.rmatch.football.core.domain.usecase

import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.analyst.AnalystInput
import com.rmatch.football.core.domain.analyst.AnalystResult
import com.rmatch.football.core.domain.analyst.MatchAnalyst
import com.rmatch.football.core.domain.analyst.TeamMatchSample
import com.rmatch.football.core.domain.analyst.TeamSample
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.StandingRow
import com.rmatch.football.core.domain.model.TeamRef
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.Loaded

data class FixtureAnalysis(
    val fixture: Fixture,
    val result: AnalystResult
)

/**
 * Collects real provider data for both teams and feeds the deterministic
 * [MatchAnalyst]. Nothing is generated when the provider has no data.
 */
class AnalyzeFixtureUseCase(
    private val repository: FootballRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend operator fun invoke(
        fixtureId: Int,
        forceRefresh: Boolean = false
    ): DataResult<FixtureAnalysis> {
        val fixtureResult = repository.fixture(fixtureId, forceRefresh)
        val fixture = when (fixtureResult) {
            is DataResult.Failure -> return fixtureResult
            is DataResult.Success -> fixtureResult.loaded.value
                ?: return DataResult.Failure(AppError.EmptyResponse)
        }

        val homeHistory = repository.lastFixturesForTeam(fixture.home.id, HISTORY_SIZE, forceRefresh)
        val awayHistory = repository.lastFixturesForTeam(fixture.away.id, HISTORY_SIZE, forceRefresh)

        val homeMatches = homeHistory.valueOrEmpty().toSamples(fixture.home.id)
        val awayMatches = awayHistory.valueOrEmpty().toSamples(fixture.away.id)

        val standings = fixture.league?.let { league ->
            league.season?.let { season ->
                repository.standings(league.id, season, forceRefresh = false).valueOrEmpty()
            }
        }.orEmpty()

        val injuries = repository.injuries(fixtureId, forceRefresh = false).valueOrEmpty()
        val lineups = repository.lineups(fixtureId, forceRefresh = false).valueOrEmpty()

        val homeLineupConfirmed = lineups.any {
            it.team?.id == fixture.home.id && it.startXI.isNotEmpty()
        }
        val awayLineupConfirmed = lineups.any {
            it.team?.id == fixture.away.id && it.startXI.isNotEmpty()
        }

        val input = AnalystInput(
            fixtureId = fixture.id,
            homeSample = buildSample(
                team = fixture.home,
                matches = homeMatches,
                standings = standings,
                injuries = injuries.count { it.teamId == fixture.home.id },
                lineupConfirmed = homeLineupConfirmed
            ),
            awaySample = buildSample(
                team = fixture.away,
                matches = awayMatches,
                standings = standings,
                injuries = injuries.count { it.teamId == fixture.away.id },
                lineupConfirmed = awayLineupConfirmed
            )
        )

        if (homeMatches.size < MatchAnalyst.MIN_MATCHES || awayMatches.size < MatchAnalyst.MIN_MATCHES) {
            return DataResult.Success(
                Loaded(
                    FixtureAnalysis(
                        fixture = fixture,
                        result = AnalystResult.Insufficient(ErrorMessages.NOT_ENOUGH_DATA)
                    ),
                    nowProvider(),
                    false
                )
            )
        }

        val analysis = MatchAnalyst.analyze(input, nowProvider())
        return DataResult.Success(
            Loaded(FixtureAnalysis(fixture, analysis), nowProvider(), false)
        )
    }

    private fun buildSample(
        team: TeamRef,
        matches: List<TeamMatchSample>,
        standings: List<StandingRow>,
        injuries: Int,
        lineupConfirmed: Boolean
    ): TeamSample {
        val row = standings.firstOrNull { it.team.id == team.id }
        return TeamSample(
            team = team,
            matches = matches,
            standingRank = row?.rank,
            standingPoints = row?.points,
            confirmedInjuries = injuries,
            lineupConfirmed = lineupConfirmed
        )
    }

    private fun <T> DataResult<List<T>>.valueOrEmpty(): List<T> = when (this) {
        is DataResult.Success -> loaded.value
        is DataResult.Failure -> emptyList()
    }

    private companion object {
        const val HISTORY_SIZE = 10
    }
}

/** Converts finished fixtures into analyst samples; unfinished matches are ignored. */
fun List<Fixture>.toSamples(teamId: Int): List<TeamMatchSample> = this
    .filter { it.status.isFinished && it.hasScore }
    .sortedByDescending { it.timestampSeconds ?: 0L }
    .mapNotNull { fixture ->
        val isHome = fixture.home.id == teamId
        val isAway = fixture.away.id == teamId
        if (!isHome && !isAway) return@mapNotNull null
        val scored = if (isHome) fixture.homeGoals else fixture.awayGoals
        val conceded = if (isHome) fixture.awayGoals else fixture.homeGoals
        if (scored == null || conceded == null) return@mapNotNull null
        TeamMatchSample(
            fixtureId = fixture.id,
            kickoffSeconds = fixture.timestampSeconds,
            isHome = isHome,
            goalsScored = scored,
            goalsConceded = conceded
        )
    }
