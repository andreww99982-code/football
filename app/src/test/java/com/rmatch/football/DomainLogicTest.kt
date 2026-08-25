package com.rmatch.football

import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.LeagueRef
import com.rmatch.football.core.domain.model.LeagueSummary
import com.rmatch.football.core.domain.model.MatchStatus
import com.rmatch.football.core.domain.model.TeamRef
import com.rmatch.football.core.domain.usecase.toSamples
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.feature.leagues.LeagueFiltering
import com.rmatch.football.feature.matches.MatchesGrouping
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {

    private fun fixture(
        id: Int,
        homeId: Int = 1,
        awayId: Int = 2,
        homeName: String = "Хозяева",
        awayName: String = "Гости",
        goalsHome: Int? = null,
        goalsAway: Int? = null,
        status: String = "NS",
        timestamp: Long? = 1_700_000_000L,
        leagueName: String = "Лига",
        country: String = "Страна"
    ) = Fixture(
        id = id,
        timestampSeconds = timestamp,
        dateIso = null,
        status = MatchStatus(status, status),
        league = LeagueRef(id = 10, name = leagueName, country = country),
        home = TeamRef(homeId, homeName),
        away = TeamRef(awayId, awayName),
        homeGoals = goalsHome,
        awayGoals = goalsAway
    )

    @Test
    fun `search filters by team, league and country`() {
        val fixtures = listOf(
            fixture(1, homeName = "Зенит"),
            fixture(2, awayName = "Спартак"),
            fixture(3, leagueName = "Премьер-лига"),
            fixture(4, country = "Испания")
        )
        assertEquals(listOf(1), MatchesGrouping.filter(fixtures, "зенит").map { it.id })
        assertEquals(listOf(2), MatchesGrouping.filter(fixtures, "спарт").map { it.id })
        assertEquals(listOf(3), MatchesGrouping.filter(fixtures, "премьер").map { it.id })
        assertEquals(listOf(4), MatchesGrouping.filter(fixtures, "испан").map { it.id })
        assertEquals(4, MatchesGrouping.filter(fixtures, "  ").size)
    }

    @Test
    fun `matches are grouped by kickoff date`() {
        val day = 24L * 60L * 60L
        val fixtures = listOf(
            fixture(1, timestamp = 1_700_000_000L),
            fixture(2, timestamp = 1_700_000_000L + day),
            fixture(3, timestamp = 1_700_000_000L + 60L)
        )
        val groups = MatchesGrouping.groupByDate(fixtures)
        assertEquals(2, groups.size)
        assertEquals(2, groups.first().second.size)
        assertEquals(listOf(1, 3), groups.first().second.map { it.id })
    }

    @Test
    fun `leagues are grouped and filtered by country`() {
        val leagues = listOf(
            LeagueSummary(
                league = LeagueRef(1, "Ла Лига", "Испания"),
                countryName = "Испания",
                countryFlagUrl = null,
                seasons = listOf(2023),
                currentSeason = 2023
            ),
            LeagueSummary(
                league = LeagueRef(2, "Серия А", "Италия"),
                countryName = "Италия",
                countryFlagUrl = null,
                seasons = listOf(2023),
                currentSeason = 2023
            )
        )
        assertEquals(1, LeagueFiltering.filter(leagues, "испан").size)
        assertEquals(1, LeagueFiltering.filter(leagues, "серия").size)
        val grouped = LeagueFiltering.groupByCountry(leagues)
        assertEquals(listOf("Испания", "Италия"), grouped.map { it.first })
    }

    @Test
    fun `only finished matches with a score become analyst samples`() {
        val fixtures = listOf(
            fixture(1, goalsHome = 2, goalsAway = 0, status = "FT", timestamp = 300L),
            fixture(2, status = "NS", timestamp = 400L),
            fixture(3, goalsHome = null, goalsAway = null, status = "FT", timestamp = 200L),
            fixture(4, homeId = 5, awayId = 6, goalsHome = 1, goalsAway = 1, status = "FT"),
            fixture(5, homeId = 2, awayId = 1, goalsHome = 0, goalsAway = 3, status = "FT",
                timestamp = 100L)
        )
        val samples = fixtures.toSamples(teamId = 1)
        assertEquals(listOf(1, 5), samples.map { it.fixtureId })

        val newest = samples.first()
        assertTrue(newest.isHome)
        assertEquals(2, newest.goalsScored)
        assertEquals(0, newest.goalsConceded)
        assertEquals(3, newest.points)
        assertEquals("В", newest.resultLetter)

        val away = samples.last()
        assertTrue(!away.isHome)
        assertEquals(3, away.goalsScored)
        assertEquals(0, away.goalsConceded)
    }

    @Test
    fun `season boundary follows the july rule`() {
        assertEquals(2024, TimeFormat.currentSeason(LocalDate.of(2024, 7, 1)))
        assertEquals(2023, TimeFormat.currentSeason(LocalDate.of(2024, 6, 30)))
        assertEquals(2024, TimeFormat.currentSeason(LocalDate.of(2025, 1, 15)))
    }

    @Test
    fun `errors are translated into the required russian messages`() {
        assertEquals(ErrorMessages.QUOTA_EXCEEDED, ErrorMessages.of(AppError.RateLimited))
        assertTrue(ErrorMessages.of(AppError.Unauthorized).isNotBlank())
        assertTrue(ErrorMessages.of(AppError.Network("нет сети")).isNotBlank())
        assertEquals("Нет верифицированных данных", ErrorMessages.NO_VERIFIED_DATA)
        assertEquals("Недостаточно данных для расчёта", ErrorMessages.NOT_ENOUGH_DATA)
        assertEquals("Коэффициенты недоступны", ErrorMessages.NO_ODDS)
        assertEquals("Лимит API исчерпан", ErrorMessages.QUOTA_EXCEEDED)
        assertEquals(
            "Составы пока не опубликованы поставщиком",
            ErrorMessages.NO_LINEUPS
        )
    }
}
