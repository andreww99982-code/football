package com.rmatch.football.core.domain.analyst

import com.rmatch.football.core.util.ErrorMessages
import kotlin.math.abs
import kotlin.math.pow

/**
 * R-Match Analyst — a fully deterministic statistical model.
 *
 * The model is a recency weighted bivariate Poisson approximation:
 *
 *  1. Only completed matches supplied by the provider are used. At least
 *     [MIN_MATCHES] completed matches are required for EACH team, otherwise the
 *     module refuses to calculate ("Недостаточно данных для расчёта").
 *  2. Every match of the sample gets a recency weight
 *        w_i = 0.5 ^ (i / HALF_LIFE_MATCHES)          (i = 0 for the newest match)
 *     so that the last matches dominate without discarding older evidence.
 *  3. The weighted scoring baseline of the sample is
 *        baseline = sum(w_i * goalsScored_i) / sum(w_i)   over BOTH teams
 *     which replaces any hardcoded "league average" constant.
 *  4. Attack and defence indices are ratios against that baseline:
 *        attack_T  = weightedGoalsScored_T   / baseline
 *        defence_T = weightedGoalsConceded_T / baseline
 *  5. Venue effect is measured on the sample itself:
 *        homeEffect = weighted goals scored at home / weighted goals scored overall
 *     clamped to [MIN_VENUE_FACTOR, MAX_VENUE_FACTOR] to avoid small sample blow ups.
 *  6. Expected goals:
 *        lambdaHome = baseline * attack_home * defence_away * homeEffect_home
 *        lambdaAway = baseline * attack_away * defence_home * awayEffect_away
 *  7. Confirmed injuries reduce the corresponding lambda by
 *        INJURY_PENALTY_PER_PLAYER each, capped at MAX_INJURY_PENALTY.
 *  8. Standings (points per match) nudge the lambdas by at most
 *        MAX_STANDING_ADJUSTMENT via a symmetric, documented factor.
 *  9. Probabilities come from [PoissonCalculator]; the 1X2 triple is normalised
 *     so that it always sums to 1.0.
 *
 * No machine learning, no random numbers, no external prediction service.
 */
object MatchAnalyst {

    const val MIN_MATCHES = 5
    const val MAX_MATCHES = 10
    const val HALF_LIFE_MATCHES = 5.0
    const val INJURY_PENALTY_PER_PLAYER = 0.03
    const val MAX_INJURY_PENALTY = 0.15
    const val MAX_STANDING_ADJUSTMENT = 0.08
    const val MIN_VENUE_FACTOR = 0.80
    const val MAX_VENUE_FACTOR = 1.25

    val TOTAL_LINES = listOf(1.5, 2.5, 3.5)
    val HANDICAP_LINES = listOf(-1.5, -0.5, 0.0, 0.5, 1.5)

    const val METHODOLOGY =
        "Модель Пуассона с весами свежести (период полураспада — 5 матчей). " +
            "Базовая результативность рассчитывается по выборке реальных завершённых матчей обеих команд, " +
            "далее применяются индексы атаки и обороны, фактор поля, подтверждённые травмы и положение в таблице. " +
            "Вероятности нормализованы, сумма исходов 1X2 равна 100%."

    fun analyze(
        input: AnalystInput,
        nowMillis: Long,
        elapsedMillisProvider: () -> Long = { System.currentTimeMillis() }
    ): AnalystResult {
        val startedAt = elapsedMillisProvider()

        val homeMatches = input.homeSample.matches.take(MAX_MATCHES)
        val awayMatches = input.awaySample.matches.take(MAX_MATCHES)

        if (homeMatches.size < MIN_MATCHES || awayMatches.size < MIN_MATCHES) {
            return AnalystResult.Insufficient(ErrorMessages.NOT_ENOUGH_DATA)
        }

        val baseline = weightedBaseline(homeMatches, awayMatches)
        if (baseline <= 0.0) {
            return AnalystResult.Insufficient(ErrorMessages.NOT_ENOUGH_DATA)
        }

        val homeAttack = weightedAverage(homeMatches) { it.goalsScored.toDouble() } / baseline
        val homeDefence = weightedAverage(homeMatches) { it.goalsConceded.toDouble() } / baseline
        val awayAttack = weightedAverage(awayMatches) { it.goalsScored.toDouble() } / baseline
        val awayDefence = weightedAverage(awayMatches) { it.goalsConceded.toDouble() } / baseline

        val homeVenueFactor = venueFactor(homeMatches, home = true)
        val awayVenueFactor = venueFactor(awayMatches, home = false)

        var lambdaHome = baseline * homeAttack * awayDefence * homeVenueFactor
        var lambdaAway = baseline * awayAttack * homeDefence * awayVenueFactor

        val standingAdjustment = standingAdjustment(input)
        lambdaHome *= (1.0 + standingAdjustment)
        lambdaAway *= (1.0 - standingAdjustment)

        lambdaHome *= (1.0 - injuryPenalty(input.homeSample.confirmedInjuries))
        lambdaAway *= (1.0 - injuryPenalty(input.awaySample.confirmedInjuries))

        lambdaHome = lambdaHome.coerceIn(0.05, 6.0)
        lambdaAway = lambdaAway.coerceIn(0.05, 6.0)

        val matrix = PoissonCalculator.scoreMatrix(lambdaHome, lambdaAway)
        val (home, draw, away) = PoissonCalculator.outcomeProbabilities(matrix)

        val totals = TOTAL_LINES.map { line ->
            val over = PoissonCalculator.overProbability(matrix, line)
            TotalLine(line = line, over = over, under = (1.0 - over).coerceIn(0.0, 1.0))
        }

        val bttsYes = PoissonCalculator.bttsProbability(matrix)

        val handicaps = HANDICAP_LINES.map { line ->
            val (cover, push, against) = PoissonCalculator.handicapProbabilities(matrix, line)
            HandicapLine(line = line, homeCovers = cover, push = push, awayCovers = against)
        }

        val quality = dataQuality(input, homeMatches.size, awayMatches.size)

        val factors = buildFactors(
            input = input,
            homeMatches = homeMatches.size,
            awayMatches = awayMatches.size,
            baseline = baseline,
            homeAttack = homeAttack,
            awayAttack = awayAttack,
            homeDefence = homeDefence,
            awayDefence = awayDefence,
            homeVenueFactor = homeVenueFactor,
            awayVenueFactor = awayVenueFactor,
            standingAdjustment = standingAdjustment
        )

        val risks = buildRisks(input, quality)

        val finishedAt = elapsedMillisProvider()

        return AnalystResult.Ready(
            AnalystReport(
                fixtureId = input.fixtureId,
                outcome = OutcomeProbabilities(home, draw, away),
                expectedGoalsHome = lambdaHome,
                expectedGoalsAway = lambdaAway,
                expectedTotalGoals = PoissonCalculator.expectedTotalGoals(matrix),
                totals = totals,
                bttsYes = bttsYes,
                bttsNo = (1.0 - bttsYes).coerceIn(0.0, 1.0),
                handicaps = handicaps,
                matchesUsedHome = homeMatches.size,
                matchesUsedAway = awayMatches.size,
                factors = factors,
                risks = risks,
                dataQuality = quality,
                computedAtMillis = nowMillis,
                computationMillis = (finishedAt - startedAt).coerceAtLeast(0L),
                methodology = METHODOLOGY
            )
        )
    }

    fun weight(index: Int): Double = 0.5.pow(index / HALF_LIFE_MATCHES)

    private fun weightedAverage(
        matches: List<TeamMatchSample>,
        selector: (TeamMatchSample) -> Double
    ): Double {
        var weighted = 0.0
        var weights = 0.0
        matches.forEachIndexed { index, sample ->
            val w = weight(index)
            weighted += w * selector(sample)
            weights += w
        }
        return if (weights <= 0.0) 0.0 else weighted / weights
    }

    private fun weightedBaseline(
        homeMatches: List<TeamMatchSample>,
        awayMatches: List<TeamMatchSample>
    ): Double {
        var weighted = 0.0
        var weights = 0.0
        listOf(homeMatches, awayMatches).forEach { matches ->
            matches.forEachIndexed { index, sample ->
                val w = weight(index)
                weighted += w * (sample.goalsScored + sample.goalsConceded) / 2.0
                weights += w
            }
        }
        return if (weights <= 0.0) 0.0 else weighted / weights
    }

    /**
     * Venue factor: how productive the team is at the relevant venue compared with
     * its overall production in the sample. Falls back to 1.0 (neutral) when the
     * sample has no match at that venue.
     */
    private fun venueFactor(matches: List<TeamMatchSample>, home: Boolean): Double {
        val venueMatches = matches.filter { it.isHome == home }
        if (venueMatches.isEmpty()) return 1.0
        val overall = weightedAverage(matches) { it.goalsScored.toDouble() }
        if (overall <= 0.0) return 1.0
        val venue = weightedAverage(venueMatches) { it.goalsScored.toDouble() }
        return (venue / overall).coerceIn(MIN_VENUE_FACTOR, MAX_VENUE_FACTOR)
    }

    /**
     * Standings adjustment: difference of points per match between both teams,
     * scaled and clamped to [MAX_STANDING_ADJUSTMENT]. Returns 0.0 when the
     * provider gives no standings data.
     */
    private fun standingAdjustment(input: AnalystInput): Double {
        val homePpm = pointsPerMatch(input.homeSample) ?: return 0.0
        val awayPpm = pointsPerMatch(input.awaySample) ?: return 0.0
        val diff = (homePpm - awayPpm) / 3.0
        return (diff * MAX_STANDING_ADJUSTMENT).coerceIn(
            -MAX_STANDING_ADJUSTMENT,
            MAX_STANDING_ADJUSTMENT
        )
    }

    private fun pointsPerMatch(sample: TeamSample): Double? {
        if (sample.matches.isEmpty()) return null
        return sample.matches.sumOf { it.points }.toDouble() / sample.matches.size
    }

    private fun injuryPenalty(confirmedInjuries: Int): Double =
        (confirmedInjuries.coerceAtLeast(0) * INJURY_PENALTY_PER_PLAYER)
            .coerceAtMost(MAX_INJURY_PENALTY)

    fun dataQuality(input: AnalystInput, homeMatches: Int, awayMatches: Int): DataQuality {
        val notes = mutableListOf<String>()
        var score = 0

        val sampleScore = ((homeMatches + awayMatches).coerceAtMost(2 * MAX_MATCHES) * 50) /
            (2 * MAX_MATCHES)
        score += sampleScore
        notes += "Матчей в выборке: хозяева $homeMatches, гости $awayMatches"

        val homeVenue = input.homeSample.matches.count { it.isHome }
        val awayVenue = input.awaySample.matches.count { !it.isHome }
        if (homeVenue >= 2 && awayVenue >= 2) {
            score += 15
            notes += "Есть разбивка по домашним и гостевым матчам"
        } else {
            notes += "Мало матчей на соответствующем поле — фактор поля ослаблен"
        }

        if (input.homeSample.standingRank != null && input.awaySample.standingRank != null) {
            score += 15
            notes += "Учтено положение команд в турнирной таблице"
        } else {
            notes += "Турнирная таблица недоступна у поставщика"
        }

        if (input.homeSample.lineupConfirmed && input.awaySample.lineupConfirmed) {
            score += 10
            notes += "Составы подтверждены поставщиком"
        } else {
            notes += "Составы пока не опубликованы поставщиком"
        }

        if (input.homeSample.confirmedInjuries > 0 || input.awaySample.confirmedInjuries > 0) {
            score += 10
            notes += "Учтены подтверждённые травмы: " +
                "${input.homeSample.confirmedInjuries} / ${input.awaySample.confirmedInjuries}"
        } else {
            notes += "Данных о травмах у поставщика нет"
        }

        return DataQuality(score = score.coerceIn(0, 100), notes = notes)
    }

    private fun buildFactors(
        input: AnalystInput,
        homeMatches: Int,
        awayMatches: Int,
        baseline: Double,
        homeAttack: Double,
        awayAttack: Double,
        homeDefence: Double,
        awayDefence: Double,
        homeVenueFactor: Double,
        awayVenueFactor: Double,
        standingAdjustment: Double
    ): List<String> = listOf(
        "Выборка: ${input.homeSample.team.name} — $homeMatches матчей, " +
            "${input.awaySample.team.name} — $awayMatches матчей",
        "Базовая результативность выборки: ${PoissonCalculator.roundTo(baseline, 2)} гола за матч",
        "Индекс атаки: ${PoissonCalculator.roundTo(homeAttack, 2)} против " +
            "${PoissonCalculator.roundTo(awayAttack, 2)}",
        "Индекс обороны: ${PoissonCalculator.roundTo(homeDefence, 2)} против " +
            "${PoissonCalculator.roundTo(awayDefence, 2)}",
        "Фактор поля: дом ${PoissonCalculator.roundTo(homeVenueFactor, 2)}, " +
            "выезд ${PoissonCalculator.roundTo(awayVenueFactor, 2)}",
        "Поправка по таблице: ${PoissonCalculator.roundTo(standingAdjustment * 100, 1)}%",
        "Подтверждённые травмы: ${input.homeSample.confirmedInjuries} / " +
            "${input.awaySample.confirmedInjuries}"
    )

    private fun buildRisks(input: AnalystInput, quality: DataQuality): List<String> {
        val risks = mutableListOf(
            "Модель статистическая: она описывает вероятности, а не гарантирует результат.",
            "Не учитываются погода, судейские решения, мотивация и внутренние новости клубов."
        )
        if (quality.score < 60) {
            risks += "Низкое качество данных: выборка мала или неполна."
        }
        if (!input.homeSample.lineupConfirmed || !input.awaySample.lineupConfirmed) {
            risks += "Стартовые составы ещё не подтверждены — расчёт может измениться."
        }
        val homePpm = pointsPerMatch(input.homeSample)
        val awayPpm = pointsPerMatch(input.awaySample)
        if (homePpm != null && awayPpm != null && abs(homePpm - awayPpm) < 0.2) {
            risks += "Команды близки по форме — распределение исходов слабо разделимо."
        }
        return risks
    }
}
