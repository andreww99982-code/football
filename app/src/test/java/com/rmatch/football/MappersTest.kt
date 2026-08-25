package com.rmatch.football

import com.rmatch.football.core.network.dto.ApiEnvelope
import com.rmatch.football.core.network.dto.FixtureDto
import com.rmatch.football.core.network.dto.LeagueDto
import com.rmatch.football.core.network.dto.OddsDto
import com.rmatch.football.core.network.mapper.parseDecimalOdd
import com.rmatch.football.core.network.mapper.toDomain
import com.rmatch.football.core.network.mapper.toDomainOrNull
import com.rmatch.football.core.network.mapper.toFixtures
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val fixturesPayload = """
        {
          "get": "fixtures",
          "parameters": {"id": "215662"},
          "errors": [],
          "results": 1,
          "paging": {"current": 1, "total": 1},
          "response": [
            {
              "fixture": {
                "id": 215662,
                "referee": "Иванов",
                "timezone": "UTC",
                "date": "2023-11-14T20:00:00+00:00",
                "timestamp": 1699992000,
                "venue": {"id": 556, "name": "Арена", "city": "Город", "capacity": 45000},
                "status": {"long": "Match Finished", "short": "FT", "elapsed": 90}
              },
              "league": {
                "id": 39, "name": "Лига", "country": "Страна",
                "logo": "https://example.org/l.png", "flag": null,
                "season": 2023, "round": "Regular Season - 12"
              },
              "teams": {
                "home": {"id": 33, "name": "Хозяева", "logo": "https://example.org/h.png"},
                "away": {"id": 34, "name": "Гости", "logo": "https://example.org/a.png"}
              },
              "goals": {"home": 2, "away": 1},
              "score": {"halftime": {"home": 1, "away": 0}, "fulltime": {"home": 2, "away": 1}}
            },
            {
              "fixture": {"id": null},
              "teams": {"home": null, "away": null}
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `fixture envelope is decoded and mapped to the domain`() {
        val envelope = json.decodeFromString(
            ApiEnvelope.serializer(ListSerializer(FixtureDto.serializer())),
            fixturesPayload
        )
        assertEquals(1, envelope.results)
        assertNotNull(envelope.response)

        val fixtures = envelope.response!!.toFixtures()
        assertEquals(1, fixtures.size)

        val fixture = fixtures.first()
        assertEquals(215662, fixture.id)
        assertEquals(1699992000L, fixture.timestampSeconds)
        assertEquals("FT", fixture.status.shortCode)
        assertTrue(fixture.status.isFinished)
        assertFalse(fixture.status.isLive)
        assertEquals("Хозяева", fixture.home.name)
        assertEquals(34, fixture.away.id)
        assertEquals(2, fixture.homeGoals)
        assertEquals(1, fixture.awayGoals)
        assertEquals(1, fixture.halftimeHome)
        assertEquals("Арена", fixture.venue?.name)
        assertEquals(45000, fixture.venue?.capacity)
        assertEquals("Иванов", fixture.referee)
        assertEquals(39, fixture.league?.id)
        assertTrue(fixture.hasScore)
        assertEquals("2 : 1", fixture.scoreLabel)
    }

    @Test
    fun `incomplete fixtures are dropped instead of being faked`() {
        val broken = json.decodeFromString(
            FixtureDto.serializer(),
            """{"fixture": {"id": 1}, "teams": {"home": {"id": 1, "name": "A"}}}"""
        )
        assertNull(broken.toDomainOrNull())
    }

    @Test
    fun `unknown status falls back to the documented default`() {
        val dto = json.decodeFromString(
            FixtureDto.serializer(),
            """
            {"fixture": {"id": 5}, "teams": {"home": {"id": 1, "name": "A"},
             "away": {"id": 2, "name": "B"}}}
            """.trimIndent()
        )
        val fixture = dto.toDomainOrNull()!!
        assertEquals("NS", fixture.status.shortCode)
        assertFalse(fixture.hasScore)
        assertEquals("—", fixture.scoreLabel)
    }

    @Test
    fun `league seasons are mapped with the current season`() {
        val dto = json.decodeFromString(
            LeagueDto.serializer(),
            """
            {
              "league": {"id": 39, "name": "Лига", "type": "League", "logo": "x"},
              "country": {"name": "Страна", "code": "ST", "flag": "f"},
              "seasons": [
                {"year": 2022, "current": false},
                {"year": 2023, "current": true}
              ]
            }
            """.trimIndent()
        )
        val league = dto.toDomainOrNull()!!
        assertEquals(39, league.league.id)
        assertEquals("Страна", league.countryName)
        assertEquals(2023, league.currentSeason)
        assertEquals(listOf(2023, 2022), league.seasons)
    }

    @Test
    fun `odds are mapped with parsed decimal values`() {
        val dto = json.decodeFromString(
            OddsDto.serializer(),
            """
            {
              "fixture": {"id": 7},
              "update": "2023-11-14T22:10:00+00:00",
              "bookmakers": [
                {"id": 8, "name": "Book", "bets": [
                  {"id": 1, "name": "Match Winner", "values": [
                    {"value": "Home", "odd": "1.90"},
                    {"value": "Draw", "odd": "3,60"},
                    {"value": "Away", "odd": "n/a"}
                  ]}
                ]}
              ]
            }
            """.trimIndent()
        )
        val board = dto.toDomain()
        assertEquals(7, board.fixtureId)
        val market = board.bookmakers.single().markets.single()
        assertEquals("Match Winner", market.name)
        assertEquals(1.90, market.selections[0].decimalOdds!!, 1e-9)
        assertEquals(3.60, market.selections[1].decimalOdds!!, 1e-9)
        assertNull(market.selections[2].decimalOdds)
    }

    @Test
    fun `decimal odd parsing rejects unusable values`() {
        assertEquals(2.5, parseDecimalOdd("2.5")!!, 1e-9)
        assertEquals(2.5, parseDecimalOdd(" 2,5 ")!!, 1e-9)
        assertNull(parseDecimalOdd(null))
        assertNull(parseDecimalOdd(""))
        assertNull(parseDecimalOdd("—"))
        assertNull(parseDecimalOdd("1.0"))
        assertNull(parseDecimalOdd("0"))
    }
}
