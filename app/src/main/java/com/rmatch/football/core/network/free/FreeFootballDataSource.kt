package com.rmatch.football.core.network.free

import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.LeagueRef
import com.rmatch.football.core.domain.model.MatchStatus
import com.rmatch.football.core.domain.model.TeamRef
import com.rmatch.football.core.domain.model.Venue
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.Loaded
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Wraps [TheSportsDbApi] and maps the raw DTO to domain [Fixture] objects.
 *
 * This source is used as a fallback when the paid API-Football quota is exhausted
 * or no key has been configured.  Data quality is lower (no odds, no lineups, no
 * detailed stats), but it provides enough info to show today's schedule.
 */
class FreeFootballDataSource(private val api: TheSportsDbApi) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun fixturesByDate(date: LocalDate): DataResult<List<Fixture>> {
        return try {
            val response = api.eventsByDay(date.format(dateFormatter))
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val events = response.body()?.events.orEmpty()
                val now = System.currentTimeMillis()
                DataResult.Success(Loaded(events.mapNotNull { it.toDomain() }, now, false))
            }
        } catch (e: Exception) {
            DataResult.Failure(AppError.Network(e.message ?: "Network error"))
        }
    }

    suspend fun fixtureById(id: Int): DataResult<Fixture?> {
        return try {
            val response = api.eventById(id)
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val event = response.body()?.events?.firstOrNull()
                val now = System.currentTimeMillis()
                DataResult.Success(Loaded(event?.toDomain(), now, false))
            }
        } catch (e: Exception) {
            DataResult.Failure(AppError.Network(e.message ?: "Network error"))
        }
    }

    private fun TheSportsDbEventDto.toDomain(): Fixture? {
        val id = idEvent?.toIntOrNull() ?: return null
        val homeName = strHomeTeam ?: return null
        val awayName = strAwayTeam ?: return null

        val ts = parseTimestamp(dateEvent, strTime)
        val homeGoals = intHomeScore?.toIntOrNull()
        val awayGoals = intAwayScore?.toIntOrNull()
        val status = mapStatus(strStatus, homeGoals, awayGoals)

        return Fixture(
            id = id,
            timestampSeconds = ts,
            dateIso = dateEvent,
            status = status,
            league = LeagueRef(
                id = idLeague?.toIntOrNull() ?: 0,
                name = strLeague ?: "",
                country = strCountry
            ),
            home = TeamRef(
                id = idHomeTeam?.toIntOrNull() ?: 0,
                name = homeName,
                logoUrl = strHomeTeamBadge
            ),
            away = TeamRef(
                id = idAwayTeam?.toIntOrNull() ?: 0,
                name = awayName,
                logoUrl = strAwayTeamBadge
            ),
            homeGoals = homeGoals,
            awayGoals = awayGoals,
            venue = if (!strVenue.isNullOrBlank()) Venue(name = strVenue, city = strCity) else null
        )
    }

    private fun parseTimestamp(date: String?, time: String?): Long? {
        if (date.isNullOrBlank()) return null
        return try {
            val timeStr = time?.trim()?.takeIf { it.isNotBlank() } ?: "00:00:00"
            val dtStr = "$date ${timeStr.padEnd(8, '0').take(8)}"
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val ldt = LocalDateTime.parse(dtStr, fmt)
            ldt.toEpochSecond(ZoneOffset.UTC)
        } catch (_: Exception) {
            null
        }
    }

    private fun mapStatus(raw: String?, homeGoals: Int?, awayGoals: Int?): MatchStatus {
        val hasScore = homeGoals != null && awayGoals != null
        return when {
            raw == null -> MatchStatus.UNKNOWN
            raw.contains("Match Finished", ignoreCase = true) ||
                raw.equals("FT", ignoreCase = true) ->
                MatchStatus("FT", "Завершён", null)

            raw.equals("In Progress", ignoreCase = true) ||
                raw.contains("HT", ignoreCase = true) ->
                MatchStatus("1H", "Идёт", null)

            raw.equals("Postponed", ignoreCase = true) ->
                MatchStatus("PST", "Отложен", null)

            raw.equals("Cancelled", ignoreCase = true) ->
                MatchStatus("CANC", "Отменён", null)

            hasScore -> MatchStatus("FT", "Завершён", null)
            else -> MatchStatus("NS", "Не начался", null)
        }
    }
}
