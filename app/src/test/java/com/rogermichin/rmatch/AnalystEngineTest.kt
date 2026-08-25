package com.rogermichin.rmatch

import com.google.common.truth.Truth.assertThat
import com.rogermichin.rmatch.data.AnalystEngine
import com.rogermichin.rmatch.data.InjuryCard
import com.rogermichin.rmatch.data.MatchSummary
import com.rogermichin.rmatch.data.OddValue
import com.rogermichin.rmatch.data.OddsMarket
import com.rogermichin.rmatch.data.StandingRow
import com.rogermichin.rmatch.data.TeamSummary
import org.junit.Test

class AnalystEngineTest {
    private val engine = AnalystEngine()
    private val home = TeamSummary(1, "Home")
    private val away = TeamSummary(2, "Away")

    @Test
    fun insufficientDataFails() {
        val result = engine.analyze(1, home, away, sampleMatches(home, away, 4), sampleMatches(away, home, 5), emptyList(), emptyList(), false, emptyList())
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun oneXTwoNormalized() {
        val analysis = engine.analyze(1, home, away, sampleMatches(home, away, 8), sampleMatches(away, home, 8), sampleStandings(), emptyList(), true, freshOdds()).getOrThrow()
        assertThat(analysis.oneXTwo.sumOf { it.probability }).isWithin(0.0001).of(1.0)
    }

    @Test
    fun totalsBttsAndHandicapsProduced() {
        val analysis = engine.analyze(1, home, away, sampleMatches(home, away, 8), sampleMatches(away, home, 8), sampleStandings(), emptyList(), true, freshOdds()).getOrThrow()
        assertThat(analysis.totals).hasSize(6)
        assertThat(analysis.bothTeamsToScore).hasSize(2)
        assertThat(analysis.handicaps).isNotEmpty()
    }

    @Test
    fun poissonProducesPositiveProbability() {
        assertThat(engine.poisson(1.5, 2)).isGreaterThan(0.0)
    }

    @Test
    fun invalidOddsRejected() {
        assertThat(runCatching { engine.impliedProbability(1.0) }.exceptionOrNull()).isNotNull()
    }

    @Test
    fun impliedAndNormalizedMarket() {
        val normalized = engine.normalizeMarket(listOf(
            OddValue("1", 2.0, engine.impliedProbability(2.0)),
            OddValue("X", 3.0, engine.impliedProbability(3.0)),
            OddValue("2", 4.0, engine.impliedProbability(4.0)),
        ))
        assertThat(normalized.sumOf { it.impliedProbability }).isWithin(0.0001).of(1.0)
    }

    @Test
    fun staleOddsSkipMarketComparison() {
        val staleOdds = listOf(OddsMarket("Book", "Match Winner", listOf(OddValue("1", 2.0, 0.5), OddValue("X", 3.0, 0.33), OddValue("2", 4.0, 0.25)), "2020-01-01T00:00:00Z", false))
        val analysis = engine.analyze(1, home, away, sampleMatches(home, away, 8), sampleMatches(away, home, 8), sampleStandings(), listOf(InjuryCard("P1", 1, "Home", "Knee", "Out")), false, staleOdds).getOrThrow()
        assertThat(analysis.marketComparisons).isEmpty()
    }

    private fun sampleMatches(primary: TeamSummary, secondary: TeamSummary, count: Int): List<MatchSummary> = (1..count).map { index ->
        MatchSummary(index, "2024-01-0${(index % 9) + 1}T12:00:00Z", 1_700_000_000 + index.toLong(), "Match Finished", "FT", 100, "League", "RU", "Round $index", "Venue", "Ref", primary, secondary, 1 + (index % 3), index % 2, 2024)
    }

    private fun sampleStandings() = listOf(
        StandingRow(1, home, 40, 20, "WWWWW", 15, 13, 1, 1, 30, 10),
        StandingRow(4, away, 29, 8, "WDLWW", 15, 9, 2, 4, 20, 12),
    )

    private fun freshOdds() = listOf(OddsMarket("Book", "Match Winner", listOf(
        OddValue("1", 2.1, engine.impliedProbability(2.1)),
        OddValue("X", 3.2, engine.impliedProbability(3.2)),
        OddValue("2", 3.8, engine.impliedProbability(3.8)),
    ), java.time.Instant.now().toString(), true))
}
