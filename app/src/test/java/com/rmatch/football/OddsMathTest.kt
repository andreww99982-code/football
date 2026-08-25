package com.rmatch.football

import com.rmatch.football.core.domain.analyst.OddsMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsMathTest {

    @Test
    fun `implied probability is the inverse of decimal odds`() {
        assertEquals(0.5, OddsMath.impliedProbability(2.0)!!, 1e-12)
        assertEquals(0.25, OddsMath.impliedProbability(4.0)!!, 1e-12)
    }

    @Test
    fun `invalid odds produce no probability`() {
        assertNull(OddsMath.impliedProbability(null))
        assertNull(OddsMath.impliedProbability(1.0))
        assertNull(OddsMath.impliedProbability(0.0))
        assertNull(OddsMath.impliedProbability(-3.0))
        assertNull(OddsMath.impliedProbability(Double.NaN))
        assertNull(OddsMath.impliedProbability(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `overround is the sum of implied probabilities minus one`() {
        val overround = OddsMath.overround(listOf(2.0, 4.0, 4.0))!!
        assertEquals(0.0, overround, 1e-12)
        val margin = OddsMath.overround(listOf(1.9, 3.6, 4.0))!!
        assertTrue(margin > 0.0)
    }

    @Test
    fun `overround is null for invalid or empty lines`() {
        assertNull(OddsMath.overround(emptyList()))
        assertNull(OddsMath.overround(listOf(2.0, null, 4.0)))
        assertNull(OddsMath.overround(listOf(2.0, 1.0)))
    }

    @Test
    fun `normalised probabilities remove the overround`() {
        val normalized = OddsMath.normalizedProbabilities(listOf(1.9, 3.6, 4.0))!!
        assertEquals(1.0, normalized.sum(), 1e-12)
        assertTrue(normalized[0] > normalized[1])
    }

    @Test
    fun `normalisation refuses partial lines`() {
        assertNull(OddsMath.normalizedProbabilities(listOf(2.0, null, 4.0)))
        assertNull(OddsMath.normalizedProbabilities(emptyList()))
    }

    @Test
    fun `market completeness requires every selection`() {
        assertTrue(OddsMath.isMarketComplete(listOf(2.0, 3.4, 3.9), 3))
        assertFalse(OddsMath.isMarketComplete(listOf(2.0, 3.4), 3))
        assertFalse(OddsMath.isMarketComplete(listOf(2.0, 3.4, null), 3))
        assertFalse(OddsMath.isMarketComplete(listOf(2.0, 3.4, 3.9), 0))
    }

    @Test
    fun `stale detection uses the freshness window`() {
        val now = 1_700_000_000_000L
        assertTrue(OddsMath.isStale(null, now))
        assertFalse(OddsMath.isStale(now - 60_000L, now))
        assertTrue(OddsMath.isStale(now - 2 * OddsMath.DEFAULT_FRESHNESS_WINDOW_MILLIS, now))
        assertFalse(OddsMath.isStale(now + 1_000L, now))
    }

    @Test
    fun `delta is model minus market`() {
        assertEquals(0.05, OddsMath.delta(0.55, 0.50), 1e-12)
        assertEquals(-0.10, OddsMath.delta(0.30, 0.40), 1e-12)
    }

    @Test
    fun `iso timestamps are parsed and invalid values rejected`() {
        assertEquals(
            1_700_000_000_000L,
            OddsMath.parseIsoToMillis("2023-11-14T22:13:20+00:00")
        )
        assertNull(OddsMath.parseIsoToMillis(null))
        assertNull(OddsMath.parseIsoToMillis(""))
        assertNull(OddsMath.parseIsoToMillis("вчера"))
    }
}
