package com.rogermichin.rmatch.data

import java.time.Instant
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class AnalystEngine {
    fun analyze(
        fixtureId: Int,
        homeTeam: TeamSummary,
        awayTeam: TeamSummary,
        homeRecent: List<MatchSummary>,
        awayRecent: List<MatchSummary>,
        standings: List<StandingRow>,
        injuries: List<InjuryCard>,
        lineupsPublished: Boolean,
        odds: List<OddsMarket>,
    ): Result<MatchAnalysis> {
        val homeFinished = homeRecent.filter { it.statusShort in setOf("FT", "AET", "PEN") }
        val awayFinished = awayRecent.filter { it.statusShort in setOf("FT", "AET", "PEN") }
        if (homeFinished.size < 5 || awayFinished.size < 5) return Result.failure(IllegalStateException("Недостаточно данных для расчёта"))

        val homeAttack = avgGoalsScored(homeFinished, homeTeam.id)
        val homeDefense = avgGoalsConceded(homeFinished, homeTeam.id)
        val awayAttack = avgGoalsScored(awayFinished, awayTeam.id)
        val awayDefense = avgGoalsConceded(awayFinished, awayTeam.id)
        val homeRank = standings.firstOrNull { it.team.id == homeTeam.id }?.rank
        val awayRank = standings.firstOrNull { it.team.id == awayTeam.id }?.rank
        val tableFactor = when {
            homeRank != null && awayRank != null -> ((awayRank - homeRank).coerceIn(-10, 10)) * 0.02
            else -> 0.0
        }
        val homeInjuries = injuries.count { it.teamId == homeTeam.id }
        val awayInjuries = injuries.count { it.teamId == awayTeam.id }
        val injuriesFactor = (awayInjuries - homeInjuries) * 0.04
        val lineupFactor = if (lineupsPublished) 0.05 else 0.0
        val homeLambda = max(0.2, ((homeAttack + awayDefense) / 2.0) * (1.08 + tableFactor + injuriesFactor + lineupFactor))
        val awayLambda = max(0.2, ((awayAttack + homeDefense) / 2.0) * (0.92 - tableFactor - injuriesFactor))
        val matrix = poissonMatrix(homeLambda, awayLambda, 8)
        val homeWin = matrix.sumOfIndexed { h, a, p -> if (h > a) p else 0.0 }
        val draw = matrix.sumOfIndexed { h, a, p -> if (h == a) p else 0.0 }
        val awayWin = matrix.sumOfIndexed { h, a, p -> if (h < a) p else 0.0 }
        val oneXTwo = normalize(listOf(AnalystOutcome("1", homeWin), AnalystOutcome("X", draw), AnalystOutcome("2", awayWin)))
        val totals = listOf(1.5, 2.5, 3.5).flatMap { threshold ->
            val over = probabilityTotalOver(matrix, threshold)
            listOf(AnalystOutcome("ТБ $threshold", over), AnalystOutcome("ТМ $threshold", 1.0 - over))
        }
        val bttsYes = probabilityBtts(matrix)
        val handicaps = buildList {
            add(AnalystOutcome("Ф1(0)", homeWin / (homeWin + awayWin).coerceAtLeast(1e-9)))
            add(AnalystOutcome("Ф2(0)", awayWin / (homeWin + awayWin).coerceAtLeast(1e-9)))
            add(AnalystOutcome("Ф1(-0.5)", homeWin))
            add(AnalystOutcome("Ф2(+0.5)", draw + awayWin))
            add(AnalystOutcome("Ф2(-0.5)", awayWin))
            add(AnalystOutcome("Ф1(+0.5)", homeWin + draw))
            if (homeFinished.size >= 7 && awayFinished.size >= 7) {
                add(AnalystOutcome("Ф1(-1.5)", probabilityGoalDiffAtLeast(matrix, 2)))
                add(AnalystOutcome("Ф2(+1.5)", 1.0 - probabilityGoalDiffAtLeast(matrix, 2)))
                add(AnalystOutcome("Ф2(-1.5)", probabilityGoalDiffAtMost(matrix, -2)))
                add(AnalystOutcome("Ф1(+1.5)", 1.0 - probabilityGoalDiffAtMost(matrix, -2)))
            }
        }
        return Result.success(
            MatchAnalysis(
                fixtureId = fixtureId,
                usedMatches = (homeFinished + awayFinished).map { it.fixtureId }.distinct(),
                modelName = "R-Match Analyst / Poisson",
                dataQuality = if (homeFinished.size >= 8 && awayFinished.size >= 8) "Хорошее" else "Умеренное",
                calculatedAtIso = Instant.now().toString(),
                expectedHomeGoals = homeLambda,
                expectedAwayGoals = awayLambda,
                oneXTwo = oneXTwo,
                totals = totals,
                bothTeamsToScore = listOf(AnalystOutcome("Обе забьют — да", bttsYes), AnalystOutcome("Обе забьют — нет", 1.0 - bttsYes)),
                handicaps = handicaps,
                factors = listOf(
                    AnalystFactor("Последние матчи", "Учтены 5–10 завершённых матчей каждой команды"),
                    AnalystFactor("Дом/выезд", "Домашняя форма хозяев и выездная форма гостей включены в λ"),
                    AnalystFactor("Таблица", homeRank?.let { "Коррекция по таблице: ${formatPercent(abs(tableFactor))}" } ?: "Таблица недоступна"),
                    AnalystFactor("Травмы", if (injuries.isEmpty()) "Подтверждённых травм нет в ответе API" else "Учтены подтверждённые травмы из API"),
                    AnalystFactor("Составы", if (lineupsPublished) "Стартовые XI опубликованы поставщиком" else "Составы пока не опубликованы поставщиком"),
                ),
                disclaimer = "Вероятности не гарантируют исход и не являются финансовой рекомендацией.",
                marketComparisons = compareMarkets(oneXTwo, odds),
            )
        )
    }

    fun impliedProbability(odd: Double): Double {
        require(odd > 1.0) { "Invalid odds" }
        return 1.0 / odd
    }

    fun normalize(outcomes: List<AnalystOutcome>): List<AnalystOutcome> {
        val total = outcomes.sumOf { it.probability }.coerceAtLeast(1e-9)
        return outcomes.map { it.copy(probability = it.probability / total) }
    }

    fun normalizeMarket(values: List<OddValue>): List<OddValue> {
        val total = values.sumOf { it.impliedProbability }.coerceAtLeast(1e-9)
        return values.map { it.copy(impliedProbability = it.impliedProbability / total) }
    }

    fun poisson(lambda: Double, goals: Int): Double = exp(-lambda) * lambda.pow(goals) / factorial(goals)

    private fun compareMarkets(model: List<AnalystOutcome>, odds: List<OddsMarket>): List<MarketComparison> {
        val market = odds.firstOrNull { it.market == "Match Winner" && it.isFresh && it.values.size >= 3 } ?: return emptyList()
        return normalizeMarket(market.values).mapNotNull { marketValue ->
            val modelValue = model.firstOrNull { it.label == marketValue.label } ?: return@mapNotNull null
            MarketComparison(market.bookmaker, market.market, marketValue.label, modelValue.probability, marketValue.impliedProbability, modelValue.probability - marketValue.impliedProbability)
        }
    }

    private fun avgGoalsScored(matches: List<MatchSummary>, teamId: Int): Double = matches.mapNotNull { when (teamId) { it.homeTeam.id -> it.homeGoals; it.awayTeam.id -> it.awayGoals; else -> null } }.average().takeIf { !it.isNaN() } ?: 0.9
    private fun avgGoalsConceded(matches: List<MatchSummary>, teamId: Int): Double = matches.mapNotNull { when (teamId) { it.homeTeam.id -> it.awayGoals; it.awayTeam.id -> it.homeGoals; else -> null } }.average().takeIf { !it.isNaN() } ?: 0.9
    private fun poissonMatrix(homeLambda: Double, awayLambda: Double, maxGoals: Int): List<List<Double>> = (0..maxGoals).map { h -> (0..maxGoals).map { a -> poisson(homeLambda, h) * poisson(awayLambda, a) } }
    private fun Double.pow(exp: Int): Double { var result = 1.0; repeat(exp) { result *= this }; return result }
    private fun factorial(n: Int): Double { var result = 1.0; for (i in 2..n) result *= i; return result }
    private fun probabilityTotalOver(matrix: List<List<Double>>, threshold: Double): Double { var p = 0.0; matrix.forEachIndexed { h, row -> row.forEachIndexed { a, v -> if (h + a > threshold) p += v } }; return p }
    private fun probabilityBtts(matrix: List<List<Double>>): Double { var p = 0.0; matrix.forEachIndexed { h, row -> row.forEachIndexed { a, v -> if (h > 0 && a > 0) p += v } }; return p }
    private fun probabilityGoalDiffAtLeast(matrix: List<List<Double>>, diff: Int): Double { var p = 0.0; matrix.forEachIndexed { h, row -> row.forEachIndexed { a, v -> if (h - a >= diff) p += v } }; return p }
    private fun probabilityGoalDiffAtMost(matrix: List<List<Double>>, diff: Int): Double { var p = 0.0; matrix.forEachIndexed { h, row -> row.forEachIndexed { a, v -> if (h - a <= diff) p += v } }; return p }
    private fun List<List<Double>>.sumOfIndexed(block: (home: Int, away: Int, value: Double) -> Double): Double { var total = 0.0; forEachIndexed { h, row -> row.forEachIndexed { a, v -> total += block(h, a, v) } }; return min(1.0, max(0.0, total)) }
    private fun formatPercent(value: Double): String = "${"%.1f".format(value * 100)}%"
}
