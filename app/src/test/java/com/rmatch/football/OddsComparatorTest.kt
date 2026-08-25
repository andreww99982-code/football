package com.rmatch.football

import com.rmatch.football.core.domain.analyst.OddsMath
import com.rmatch.football.core.domain.analyst.OutcomeProbabilities
import com.rmatch.football.core.domain.model.OddsBoard
import com.rmatch.football.core.domain.model.OddsBookmaker
import com.rmatch.football.core.domain.model.OddsMarket
import com.rmatch.football.core.domain.model.OddsSelection
import com.rmatch.football.core.domain.usecase.OddsComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsComparatorTest {

    private val now = 1_700_000_000_000L
    private val freshIso = "2023-11-14T22:10:00+00:00"
    private val staleIso = "2023-11-10T10:00:00+00:00"

    private fun board(
        updateIso: String?,
        home: String? = "1.90",
        draw: String? = "3.60",
        away: String? = "4.00"
    ) = OddsBoard(
        fixtureId = 7,
        updateIso = updateIso,
        bookmakers = listOf(
            OddsBookmaker(
                id = 8,
                name = "Bookmaker A",
                markets = listOf(
                    OddsMarket(
                        id = 1,
                        name = "Match Winner",
                        selections = listOfNotNull(
                            home?.let { OddsSelection("Home", it, it.toDouble()) },
                            draw?.let { OddsSelection("Draw", it, it.toDouble()) },
                            away?.let { OddsSelection("Away", it, it.toDouble()) }
                        )
                    )
                )
            )
        )
    )

    private val model = OutcomeProbabilities(homeWin = 0.55, draw = 0.25, awayWin = 0.20)

    @Test
    fun `fresh and complete market exposes implied, normalised and delta values`() {
        val comparison = OddsComparator.compareMatchWinner(board(freshIso), model, now).single()
        assertEquals("Bookmaker A", comparison.bookmakerName)
        assertEquals(OddsComparator.PROVIDER, comparison.provider)
        assertFalse(comparison.isStale)
        assertTrue(comparison.isComplete)
        assertNotNull(comparison.overround)
        assertTrue(comparison.overround!! > 0.0)

        assertEquals(3, comparison.rows.size)
        val row = comparison.rows.first()
        assertEquals(1.0 / 1.90, row.impliedProbability!!, 1e-12)
        assertEquals(1.0, comparison.rows.sumOf { it.normalizedProbability ?: 0.0 }, 1e-12)
        assertEquals(
            OddsMath.delta(0.55, comparison.rows[0].normalizedProbability!!),
            comparison.rows[0].delta!!,
            1e-12
        )
    }

    @Test
    fun `stale odds hide the delta but keep the numbers`() {
        val comparison = OddsComparator.compareMatchWinner(board(staleIso), model, now).single()
        assertTrue(comparison.isStale)
        assertTrue(comparison.rows.all { it.delta == null })
        assertNotNull(comparison.rows[0].impliedProbability)
    }

    @Test
    fun `incomplete market is not normalised and has no delta`() {
        val comparison =
            OddsComparator.compareMatchWinner(board(freshIso, draw = null), model, now).single()
        assertFalse(comparison.isComplete)
        assertNull(comparison.overround)
        assertTrue(comparison.rows.all { it.normalizedProbability == null })
        assertTrue(comparison.rows.all { it.delta == null })
    }

    @Test
    fun `missing model output removes the delta only`() {
        val comparison = OddsComparator.compareMatchWinner(board(freshIso), null, now).single()
        assertTrue(comparison.isComplete)
        assertTrue(comparison.rows.all { it.delta == null })
        assertTrue(comparison.rows.all { it.normalizedProbability != null })
    }

    @Test
    fun `unknown markets are skipped`() {
        val other = OddsBoard(
            fixtureId = 7,
            updateIso = freshIso,
            bookmakers = listOf(
                OddsBookmaker(
                    id = 1,
                    name = "B",
                    markets = listOf(
                        OddsMarket(2, "Both Teams Score", listOf(OddsSelection("Yes", "1.8", 1.8)))
                    )
                )
            )
        )
        assertTrue(OddsComparator.compareMatchWinner(other, model, now).isEmpty())
    }
}
