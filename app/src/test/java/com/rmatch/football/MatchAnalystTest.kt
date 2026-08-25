package com.rmatch.football

import com.rmatch.football.core.domain.analyst.AnalystInput
import com.rmatch.football.core.domain.analyst.AnalystResult
import com.rmatch.football.core.domain.analyst.MatchAnalyst
import com.rmatch.football.core.domain.analyst.TeamMatchSample
import com.rmatch.football.core.domain.analyst.TeamSample
import com.rmatch.football.core.domain.model.TeamRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchAnalystTest {

    private fun samples(
        count: Int,
        scored: Int,
        conceded: Int,
        startId: Int = 1
    ): List<TeamMatchSample> = (0 until count).map { index ->
        TeamMatchSample(
            fixtureId = startId + index,
            kickoffSeconds = 1_700_000_000L - index * 604_800L,
            isHome = index % 2 == 0,
            goalsScored = scored,
            goalsConceded = conceded
        )
    }

    private fun input(
        homeMatches: List<TeamMatchSample>,
        awayMatches: List<TeamMatchSample>,
        homeInjuries: Int = 0,
        awayInjuries: Int = 0,
        homeRank: Int? = null,
        awayRank: Int? = null
    ) = AnalystInput(
        fixtureId = 42,
        homeSample = TeamSample(
            team = TeamRef(1, "Хозяева"),
            matches = homeMatches,
            standingRank = homeRank,
            confirmedInjuries = homeInjuries
        ),
        awaySample = TeamSample(
            team = TeamRef(2, "Гости"),
            matches = awayMatches,
            standingRank = awayRank,
            confirmedInjuries = awayInjuries
        )
    )

    @Test
    fun `refuses to calculate without the minimum sample`() {
        val result = MatchAnalyst.analyze(
            input(samples(4, 2, 1), samples(9, 1, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(result is AnalystResult.Insufficient)
        assertEquals(
            "Недостаточно данных для расчёта",
            (result as AnalystResult.Insufficient).reason
        )
    }

    @Test
    fun `refuses when the away team has too few matches`() {
        val result = MatchAnalyst.analyze(
            input(samples(8, 2, 1), samples(2, 1, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(result is AnalystResult.Insufficient)
    }

    @Test
    fun `1X2 probabilities are normalised`() {
        val result = MatchAnalyst.analyze(
            input(samples(10, 2, 1), samples(10, 1, 2, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        val report = (result as AnalystResult.Ready).report
        val sum = report.outcome.homeWin + report.outcome.draw + report.outcome.awayWin
        assertEquals(1.0, sum, 1e-9)
        assertTrue(report.outcome.homeWin > report.outcome.awayWin)
    }

    @Test
    fun `totals and btts are complementary`() {
        val result = MatchAnalyst.analyze(
            input(samples(6, 2, 1), samples(6, 1, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        val report = (result as AnalystResult.Ready).report
        assertEquals(3, report.totals.size)
        report.totals.forEach { total ->
            assertEquals(1.0, total.over + total.under, 1e-9)
        }
        assertEquals(1.0, report.bttsYes + report.bttsNo, 1e-9)
        val over15 = report.totals.first { it.line == 1.5 }.over
        val over35 = report.totals.first { it.line == 3.5 }.over
        assertTrue(over15 > over35)
    }

    @Test
    fun `handicap lines are produced and normalised`() {
        val result = MatchAnalyst.analyze(
            input(samples(7, 2, 1), samples(7, 1, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        val report = (result as AnalystResult.Ready).report
        assertEquals(MatchAnalyst.HANDICAP_LINES.size, report.handicaps.size)
        report.handicaps.forEach { line ->
            assertEquals(1.0, line.homeCovers + line.push + line.awayCovers, 1e-9)
        }
    }

    @Test
    fun `report exposes sample size, factors, quality and timing`() {
        val now = 1_700_000_000_000L
        var clock = 0L
        val result = MatchAnalyst.analyze(
            input(samples(9, 2, 1), samples(6, 1, 1, startId = 100)),
            nowMillis = now,
            elapsedMillisProvider = { clock += 5L; clock }
        )
        val report = (result as AnalystResult.Ready).report
        assertEquals(9, report.matchesUsedHome)
        assertEquals(6, report.matchesUsedAway)
        assertEquals(now, report.computedAtMillis)
        assertTrue(report.computationMillis >= 0L)
        assertTrue(report.factors.isNotEmpty())
        assertTrue(report.dataQuality.score in 0..100)
        assertEquals(MatchAnalyst.METHODOLOGY, report.methodology)
    }

    @Test
    fun `sample is capped at the documented maximum`() {
        val result = MatchAnalyst.analyze(
            input(samples(25, 2, 1), samples(25, 1, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        )
        val report = (result as AnalystResult.Ready).report
        assertEquals(MatchAnalyst.MAX_MATCHES, report.matchesUsedHome)
        assertEquals(MatchAnalyst.MAX_MATCHES, report.matchesUsedAway)
    }

    @Test
    fun `confirmed injuries reduce the expected goals of that team`() {
        val healthy = MatchAnalyst.analyze(
            input(samples(8, 2, 1), samples(8, 2, 1, startId = 100)),
            nowMillis = 1_700_000_000_000L
        ) as AnalystResult.Ready
        val injured = MatchAnalyst.analyze(
            input(samples(8, 2, 1), samples(8, 2, 1, startId = 100), homeInjuries = 3),
            nowMillis = 1_700_000_000_000L
        ) as AnalystResult.Ready
        assertTrue(injured.report.expectedGoalsHome < healthy.report.expectedGoalsHome)
    }

    @Test
    fun `recency weight decays monotonically`() {
        assertEquals(1.0, MatchAnalyst.weight(0), 1e-12)
        assertEquals(0.5, MatchAnalyst.weight(5), 1e-12)
        assertTrue(MatchAnalyst.weight(1) > MatchAnalyst.weight(4))
    }

    @Test
    fun `model is deterministic for identical input`() {
        val first = MatchAnalyst.analyze(
            input(samples(10, 3, 1), samples(10, 1, 2, startId = 100)),
            nowMillis = 1L
        ) as AnalystResult.Ready
        val second = MatchAnalyst.analyze(
            input(samples(10, 3, 1), samples(10, 1, 2, startId = 100)),
            nowMillis = 1L
        ) as AnalystResult.Ready
        assertEquals(first.report.outcome, second.report.outcome)
        assertEquals(first.report.expectedGoalsHome, second.report.expectedGoalsHome, 0.0)
    }
}
