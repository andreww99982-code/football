package com.rmatch.football.core.domain.analyst

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

/**
 * Deterministic Poisson helper used by the R-Match Analyst.
 *
 * Formulas (documented, no AI, no randomness):
 *   P(X = k) = exp(-lambda) * lambda^k / k!
 * Score matrix assumes conditional independence of the two team scoring
 * processes: P(h, a) = P(X = h | lambdaHome) * P(Y = a | lambdaAway).
 * The matrix is truncated at [MAX_GOALS] and renormalised so that the sum of
 * all cells equals exactly 1.0 (truncation mass is redistributed proportionally).
 */
object PoissonCalculator {

    const val MAX_GOALS = 10

    /** Poisson probability mass function, computed in log space for stability. */
    fun probability(lambda: Double, k: Int): Double {
        if (k < 0) return 0.0
        val safeLambda = lambda.coerceIn(MIN_LAMBDA, MAX_LAMBDA)
        var logFactorial = 0.0
        for (i in 2..k) logFactorial += ln(i.toDouble())
        val logP = -safeLambda + k * ln(safeLambda) - logFactorial
        return exp(logP)
    }

    /** Normalised joint distribution of the final score. */
    fun scoreMatrix(
        lambdaHome: Double,
        lambdaAway: Double,
        maxGoals: Int = MAX_GOALS
    ): Array<DoubleArray> {
        val size = maxGoals + 1
        val home = DoubleArray(size) { probability(lambdaHome, it) }
        val away = DoubleArray(size) { probability(lambdaAway, it) }
        val matrix = Array(size) { h -> DoubleArray(size) { a -> home[h] * away[a] } }
        val total = matrix.sumOf { row -> row.sum() }
        if (total <= 0.0) return matrix
        for (h in 0 until size) {
            for (a in 0 until size) {
                matrix[h][a] = matrix[h][a] / total
            }
        }
        return matrix
    }

    /** Returns home win / draw / away win probabilities (they always sum to 1.0). */
    fun outcomeProbabilities(matrix: Array<DoubleArray>): Triple<Double, Double, Double> {
        var home = 0.0
        var draw = 0.0
        var away = 0.0
        for (h in matrix.indices) {
            for (a in matrix[h].indices) {
                val p = matrix[h][a]
                when {
                    h > a -> home += p
                    h == a -> draw += p
                    else -> away += p
                }
            }
        }
        return normalizeTriple(home, draw, away)
    }

    /** Probability that total goals are strictly greater than [line] (e.g. 2.5). */
    fun overProbability(matrix: Array<DoubleArray>, line: Double): Double {
        var over = 0.0
        for (h in matrix.indices) {
            for (a in matrix[h].indices) {
                if (h + a > line) over += matrix[h][a]
            }
        }
        return over.coerceIn(0.0, 1.0)
    }

    /** Probability that both teams score at least one goal. */
    fun bttsProbability(matrix: Array<DoubleArray>): Double {
        var yes = 0.0
        for (h in 1 until matrix.size) {
            for (a in 1 until matrix[h].size) {
                yes += matrix[h][a]
            }
        }
        return yes.coerceIn(0.0, 1.0)
    }

    /**
     * Asian style handicap applied to the home team.
     * Home covers when (homeGoals + line) > awayGoals, push when equal.
     */
    fun handicapProbabilities(
        matrix: Array<DoubleArray>,
        line: Double
    ): Triple<Double, Double, Double> {
        var homeCover = 0.0
        var push = 0.0
        var awayCover = 0.0
        for (h in matrix.indices) {
            for (a in matrix[h].indices) {
                val margin = h + line - a
                when {
                    margin > 0.0 -> homeCover += matrix[h][a]
                    margin == 0.0 -> push += matrix[h][a]
                    else -> awayCover += matrix[h][a]
                }
            }
        }
        return Triple(
            homeCover.coerceIn(0.0, 1.0),
            push.coerceIn(0.0, 1.0),
            awayCover.coerceIn(0.0, 1.0)
        )
    }

    /** Expected number of goals of the modelled distribution. */
    fun expectedTotalGoals(matrix: Array<DoubleArray>): Double {
        var expected = 0.0
        for (h in matrix.indices) {
            for (a in matrix[h].indices) {
                expected += (h + a) * matrix[h][a]
            }
        }
        return expected
    }

    fun normalizeTriple(a: Double, b: Double, c: Double): Triple<Double, Double, Double> {
        val sum = a + b + c
        if (sum <= 0.0) return Triple(0.0, 0.0, 0.0)
        return Triple(a / sum, b / sum, c / sum)
    }

    fun roundTo(value: Double, decimals: Int): Double {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        return round(value * factor) / factor
    }

    private const val MIN_LAMBDA = 0.05
    private const val MAX_LAMBDA = 8.0
}
