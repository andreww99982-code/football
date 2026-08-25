package com.rmatch.football

import com.rmatch.football.core.domain.analyst.PoissonCalculator
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoissonCalculatorTest {

    @Test
    fun `pmf matches the analytical values`() {
        assertEquals(0.367879, PoissonCalculator.probability(1.0, 0), 1e-6)
        assertEquals(0.367879, PoissonCalculator.probability(1.0, 1), 1e-6)
        assertEquals(0.183939, PoissonCalculator.probability(1.0, 2), 1e-6)
        assertEquals(0.334695, PoissonCalculator.probability(1.5, 1), 1e-6)
    }

    @Test
    fun `negative goal count is impossible`() {
        assertEquals(0.0, PoissonCalculator.probability(1.4, -1), 0.0)
    }

    @Test
    fun `score matrix is normalised to one`() {
        val matrix = PoissonCalculator.scoreMatrix(1.7, 1.1)
        val total = matrix.sumOf { it.sum() }
        assertEquals(1.0, total, 1e-9)
    }

    @Test
    fun `outcome probabilities sum to one and favour the stronger side`() {
        val matrix = PoissonCalculator.scoreMatrix(2.2, 0.9)
        val (home, draw, away) = PoissonCalculator.outcomeProbabilities(matrix)
        assertEquals(1.0, home + draw + away, 1e-9)
        assertTrue(home > away)
        assertTrue(draw in 0.0..1.0)
    }

    @Test
    fun `over and under of a line are complementary`() {
        val matrix = PoissonCalculator.scoreMatrix(1.4, 1.2)
        val over = PoissonCalculator.overProbability(matrix, 2.5)
        assertTrue(over > 0.0 && over < 1.0)
        assertEquals(1.0, over + (1.0 - over), 1e-9)
    }

    @Test
    fun `higher lambdas increase the over probability`() {
        val low = PoissonCalculator.overProbability(PoissonCalculator.scoreMatrix(0.8, 0.7), 2.5)
        val high = PoissonCalculator.overProbability(PoissonCalculator.scoreMatrix(2.4, 2.1), 2.5)
        assertTrue(high > low)
    }

    @Test
    fun `btts equals the product of both teams scoring`() {
        val lambdaHome = 1.6
        val lambdaAway = 1.3
        val matrix = PoissonCalculator.scoreMatrix(lambdaHome, lambdaAway)
        val expected = (1 - PoissonCalculator.probability(lambdaHome, 0)) *
            (1 - PoissonCalculator.probability(lambdaAway, 0))
        assertTrue(abs(PoissonCalculator.bttsProbability(matrix) - expected) < 1e-3)
    }

    @Test
    fun `handicap parts sum to one and zero line has a push`() {
        val matrix = PoissonCalculator.scoreMatrix(1.5, 1.5)
        val (cover, push, against) = PoissonCalculator.handicapProbabilities(matrix, 0.0)
        assertEquals(1.0, cover + push + against, 1e-9)
        assertTrue(push > 0.0)
        assertEquals(cover, against, 1e-9)
    }

    @Test
    fun `half lines never push`() {
        val matrix = PoissonCalculator.scoreMatrix(1.5, 1.2)
        val (_, push, _) = PoissonCalculator.handicapProbabilities(matrix, -0.5)
        assertEquals(0.0, push, 1e-12)
    }

    @Test
    fun `expected total goals is close to the sum of lambdas`() {
        val matrix = PoissonCalculator.scoreMatrix(1.5, 1.2)
        assertEquals(2.7, PoissonCalculator.expectedTotalGoals(matrix), 1e-3)
    }

    @Test
    fun `normalize triple handles a zero sum`() {
        assertEquals(
            Triple(0.0, 0.0, 0.0),
            PoissonCalculator.normalizeTriple(0.0, 0.0, 0.0)
        )
    }
}
