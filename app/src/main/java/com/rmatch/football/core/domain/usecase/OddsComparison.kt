package com.rmatch.football.core.domain.usecase

import com.rmatch.football.core.domain.analyst.OddsMath
import com.rmatch.football.core.domain.analyst.OutcomeProbabilities
import com.rmatch.football.core.domain.model.OddsBoard

/** One selection line of a market, with neutral informational metrics only. */
data class MarketRow(
    val label: String,
    val decimalOdds: Double?,
    val impliedProbability: Double?,
    val normalizedProbability: Double?,
    val modelProbability: Double?,
    val delta: Double?
)

data class MarketComparison(
    val bookmakerName: String,
    val marketName: String,
    val provider: String,
    val updatedAtMillis: Long?,
    val isStale: Boolean,
    val isComplete: Boolean,
    val overround: Double?,
    val rows: List<MarketRow>
)

/**
 * Converts provider odds into probabilities and compares them with the model.
 * The delta is only exposed when the market line is complete AND fresh.
 */
object OddsComparator {

    const val PROVIDER = "API-Football"
    const val MARKET_MATCH_WINNER = "Match Winner"

    private val HOME_LABELS = setOf("home", "1")
    private val DRAW_LABELS = setOf("draw", "x")
    private val AWAY_LABELS = setOf("away", "2")

    fun compareMatchWinner(
        board: OddsBoard,
        model: OutcomeProbabilities?,
        nowMillis: Long,
        freshnessWindowMillis: Long = OddsMath.DEFAULT_FRESHNESS_WINDOW_MILLIS
    ): List<MarketComparison> {
        val updatedAt = OddsMath.parseIsoToMillis(board.updateIso)
        val stale = OddsMath.isStale(updatedAt, nowMillis, freshnessWindowMillis)

        return board.bookmakers.mapNotNull { bookmaker ->
            val market = bookmaker.markets.firstOrNull {
                it.name.equals(MARKET_MATCH_WINNER, ignoreCase = true)
            } ?: return@mapNotNull null

            val home = market.selections.firstOrNull { it.label.normalized() in HOME_LABELS }
            val draw = market.selections.firstOrNull { it.label.normalized() in DRAW_LABELS }
            val away = market.selections.firstOrNull { it.label.normalized() in AWAY_LABELS }

            val ordered = listOf(home, draw, away)
            val odds = ordered.map { it?.decimalOdds }
            val complete = OddsMath.isMarketComplete(odds, expectedSelections = 3)
            val normalized = if (complete) OddsMath.normalizedProbabilities(odds) else null
            val modelValues = listOf(model?.homeWin, model?.draw, model?.awayWin)
            val labels = listOf("П1", "Х", "П2")

            val rows = labels.mapIndexed { index, label ->
                val decimal = odds[index]
                val implied = OddsMath.impliedProbability(decimal)
                val market1 = normalized?.getOrNull(index)
                val modelValue = modelValues[index]
                val delta = if (!stale && complete && market1 != null && modelValue != null) {
                    OddsMath.delta(modelValue, market1)
                } else {
                    null
                }
                MarketRow(
                    label = label,
                    decimalOdds = decimal,
                    impliedProbability = implied,
                    normalizedProbability = market1,
                    modelProbability = modelValue,
                    delta = delta
                )
            }

            MarketComparison(
                bookmakerName = bookmaker.name,
                marketName = market.name,
                provider = PROVIDER,
                updatedAtMillis = updatedAt,
                isStale = stale,
                isComplete = complete,
                overround = if (complete) OddsMath.overround(odds) else null,
                rows = rows
            )
        }
    }

    private fun String.normalized(): String = trim().lowercase()
}
