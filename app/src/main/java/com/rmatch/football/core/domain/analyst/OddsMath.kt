package com.rmatch.football.core.domain.analyst

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Neutral odds arithmetic. Nothing here recommends a bet: it only converts the
 * provider numbers into probabilities so that the model output can be compared
 * with the market in an informative, non-promotional way.
 */
object OddsMath {

    /** Odds older than this are shown as stale and excluded from delta output. */
    const val DEFAULT_FRESHNESS_WINDOW_MILLIS = 60L * 60L * 1000L

    /** implied probability = 1 / decimal odds; invalid odds return null. */
    fun impliedProbability(decimalOdds: Double?): Double? {
        if (decimalOdds == null) return null
        if (decimalOdds.isNaN() || decimalOdds.isInfinite()) return null
        if (decimalOdds <= 1.0) return null
        return 1.0 / decimalOdds
    }

    /** Bookmaker margin: sum of implied probabilities minus 1. Null when the line is invalid. */
    fun overround(decimalOdds: List<Double?>): Double? {
        if (decimalOdds.isEmpty()) return null
        var sum = 0.0
        for (odd in decimalOdds) {
            val implied = impliedProbability(odd) ?: return null
            sum += implied
        }
        return sum - 1.0
    }

    /**
     * Market probabilities with the overround removed (proportional / multiplicative
     * normalisation). Returns null when any selection is invalid, because a partial
     * line cannot be normalised honestly.
     */
    fun normalizedProbabilities(decimalOdds: List<Double?>): List<Double>? {
        if (decimalOdds.isEmpty()) return null
        val implied = decimalOdds.map { impliedProbability(it) ?: return null }
        val sum = implied.sum()
        if (sum <= 0.0) return null
        return implied.map { it / sum }
    }

    /** A market is usable only when every expected selection is present and valid. */
    fun isMarketComplete(decimalOdds: List<Double?>, expectedSelections: Int): Boolean {
        if (expectedSelections <= 0) return false
        if (decimalOdds.size != expectedSelections) return false
        return decimalOdds.all { impliedProbability(it) != null }
    }

    fun isStale(
        updatedAtMillis: Long?,
        nowMillis: Long,
        windowMillis: Long = DEFAULT_FRESHNESS_WINDOW_MILLIS
    ): Boolean {
        if (updatedAtMillis == null) return true
        if (updatedAtMillis > nowMillis) return false
        return nowMillis - updatedAtMillis > windowMillis
    }

    /** Difference between the statistical model and the de-vigged market. */
    fun delta(modelProbability: Double, marketProbability: Double): Double =
        modelProbability - marketProbability

    /** Parses the ISO-8601 timestamps returned by the provider. */
    fun parseIsoToMillis(iso: String?): Long? {
        val value = iso?.trim().orEmpty()
        if (value.isEmpty()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
