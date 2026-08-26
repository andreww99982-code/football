package com.rmatch.football.core.network.free

import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.LeagueRef
import com.rmatch.football.core.domain.model.LeagueSummary
import com.rmatch.football.core.domain.model.MatchStatus
import com.rmatch.football.core.domain.model.StandingRow
import com.rmatch.football.core.domain.model.TeamRef
import com.rmatch.football.core.domain.model.Venue
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.Loaded
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class OpenLigaDbDataSource(private val api: OpenLigaDbApi) {

    private val leagueShortcuts = ConcurrentHashMap<Int, String>()
    private val knownFixtureIds = ConcurrentHashMap.newKeySet<Int>()

    suspend fun leagues(season: Int): DataResult<List<LeagueSummary>> {
        return try {
            val response = api.availableLeagues()
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val now = System.currentTimeMillis()
                val leagues = response.body()
                    .asArray()
                    .mapNotNull { it.asObjectOrNull()?.toLeagueSummary(requestedSeason = season) }
                    .distinctBy { it.league.id }
                    .sortedBy { it.league.name }
                DataResult.Success(Loaded(leagues, now, false))
            }
        } catch (error: Exception) {
            DataResult.Failure(AppError.Network(error.message ?: "Network error"))
        }
    }

    suspend fun nextFixturesForLeague(
        leagueId: Int,
        season: Int,
        next: Int
    ): DataResult<List<Fixture>> {
        val shortcut = leagueShortcuts[leagueId]
            ?: return DataResult.Failure(AppError.EmptyResponse)
        return try {
            val response = api.matchesByLeague(shortcut, season)
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val now = System.currentTimeMillis()
                val upcoming = response.body()
                    .asArray()
                    .mapNotNull { it.asObjectOrNull()?.toFixture(localLeagueId = leagueId) }
                    .onEach { knownFixtureIds += it.id }
                    .sortedWith(compareBy<Fixture> { it.timestampSeconds ?: Long.MAX_VALUE }.thenBy { it.id })
                    .let { fixtures ->
                        val filtered = fixtures.filter { !it.status.isFinished }
                        if (filtered.isNotEmpty()) filtered.take(next) else fixtures.takeLast(next)
                    }
                DataResult.Success(Loaded(upcoming, now, false))
            }
        } catch (error: Exception) {
            DataResult.Failure(AppError.Network(error.message ?: "Network error"))
        }
    }

    suspend fun standings(leagueId: Int, season: Int): DataResult<List<StandingRow>> {
        val shortcut = leagueShortcuts[leagueId]
            ?: return DataResult.Failure(AppError.EmptyResponse)
        return try {
            val response = api.standings(shortcut, season)
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val now = System.currentTimeMillis()
                val table = response.body()
                    .asArray()
                    .mapIndexedNotNull { index, entry ->
                        entry.asObjectOrNull()?.toStandingRow(rank = index + 1)
                    }
                DataResult.Success(Loaded(table, now, false))
            }
        } catch (error: Exception) {
            DataResult.Failure(AppError.Network(error.message ?: "Network error"))
        }
    }

    suspend fun fixtureById(fixtureId: Int): DataResult<Fixture?> {
        return try {
            val response = api.matchById(fixtureId)
            if (!response.isSuccessful) {
                DataResult.Failure(AppError.Http(response.code()))
            } else {
                val root = response.body()
                val match = when (root) {
                    is JsonObject -> root
                    is JsonArray -> root.firstOrNull()?.asObjectOrNull()
                    else -> null
                }
                val now = System.currentTimeMillis()
                DataResult.Success(Loaded(match?.toFixture(), now, false))
            }
        } catch (error: Exception) {
            DataResult.Failure(AppError.Network(error.message ?: "Network error"))
        }
    }

    fun canHandleLeague(leagueId: Int): Boolean = leagueShortcuts.containsKey(leagueId)

    fun canHandleFixture(fixtureId: Int): Boolean = knownFixtureIds.contains(fixtureId)

    private fun JsonObject.toLeagueSummary(requestedSeason: Int): LeagueSummary? {
        val shortcut = string("leagueShortcut")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val season = int("leagueSeason", "leagueSaison") ?: requestedSeason
        if (season != requestedSeason) return null
        val localId = localLeagueId(shortcut)
        leagueShortcuts[localId] = shortcut
        return LeagueSummary(
            league = LeagueRef(
                id = localId,
                name = string("leagueName") ?: shortcut.uppercase(),
                country = "OpenLigaDB",
                season = season
            ),
            countryName = "OpenLigaDB",
            countryFlagUrl = null,
            seasons = listOf(season),
            currentSeason = season
        )
    }

    private fun JsonObject.toFixture(localLeagueId: Int? = null): Fixture? {
        val id = int("matchID", "matchId") ?: return null
        val home = objectValue("team1")
        val away = objectValue("team2")
        val homeName = home?.string("teamName", "teamInfoName") ?: return null
        val awayName = away?.string("teamName", "teamInfoName") ?: return null
        val timestamp = parseUtc(string("matchDateTimeUTC", "matchDateTime"))
        val results = arrayValue("matchResults").orEmpty()
        val finalResult = results.maxByOrNull {
            it.asObjectOrNull()?.int("resultOrderID", "resultOrderId", "resultTypeID", "resultTypeId") ?: 0
        }?.asObjectOrNull()
        val homeGoals = finalResult?.int("pointsTeam1")
        val awayGoals = finalResult?.int("pointsTeam2")
        val finished = boolean("matchIsFinished") ?: false
        val status = when {
            finished -> MatchStatus("FT", "Завершён", null)
            timestamp != null && timestamp <= Instant.now().epochSecond -> MatchStatus("LIVE", "Идёт", null)
            else -> MatchStatus.UNKNOWN
        }
        val leagueName = string("leagueName")
        val shortcut = string("leagueShortcut")
        val season = int("leagueSeason", "leagueSaison")
        val leagueId = localLeagueId ?: shortcut?.let(::localLeagueId) ?: int("leagueId") ?: 0
        shortcut?.let { leagueShortcuts[leagueId] = it }
        return Fixture(
            id = id,
            timestampSeconds = timestamp,
            dateIso = string("matchDateTimeUTC", "matchDateTime"),
            status = status,
            league = LeagueRef(
                id = leagueId,
                name = leagueName ?: "OpenLigaDB",
                country = "OpenLigaDB",
                season = season
            ),
            home = TeamRef(
                id = home?.int("teamId", "teamInfoId") ?: 0,
                name = homeName,
                logoUrl = home?.string("teamIconUrl")
            ),
            away = TeamRef(
                id = away?.int("teamId", "teamInfoId") ?: 0,
                name = awayName,
                logoUrl = away?.string("teamIconUrl")
            ),
            homeGoals = homeGoals,
            awayGoals = awayGoals,
            venue = objectValue("location")?.let { location ->
                Venue(
                    name = location.string("locationStadium", "locationName"),
                    city = location.string("locationCity")
                )
            }
        )
    }

    private fun JsonObject.toStandingRow(rank: Int): StandingRow? {
        val teamName = string("teamName", "shortName") ?: return null
        val goalsFor = int("goals") ?: 0
        val goalsAgainst = int("opponentGoals") ?: 0
        return StandingRow(
            rank = rank,
            team = TeamRef(
                id = int("teamInfoId", "teamId") ?: 0,
                name = teamName,
                logoUrl = string("teamIconUrl")
            ),
            points = int("points") ?: 0,
            goalsDiff = int("goalDiff") ?: (goalsFor - goalsAgainst),
            group = null,
            form = null,
            played = int("matches") ?: 0,
            win = int("won") ?: 0,
            draw = int("draw") ?: 0,
            lose = int("lost") ?: 0,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            description = null
        )
    }

    private fun localLeagueId(shortcut: String): Int = -kotlin.math.abs(shortcut.hashCode().coerceAtLeast(1))

    private fun parseUtc(raw: String?): Long? = raw?.takeIf { it.isNotBlank() }?.let {
        runCatching { Instant.parse(normalizeUtc(it)).epochSecond }.getOrNull()
    }

    private fun normalizeUtc(value: String): String {
        val trimmed = value.trim().replace(' ', 'T')
        return when {
            trimmed.endsWith("Z") -> trimmed
            else -> "${trimmed}${if (trimmed.contains("T")) "Z" else "T00:00:00Z"}"
        }
    }
}

private fun JsonElement?.asArray(): List<JsonElement> = (this as? JsonArray).orEmpty()

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { key ->
        when (val value = this[key]) {
            is JsonPrimitive -> value.intOrNull ?: value.contentOrNull?.toIntOrNull()
            else -> null
        }
    }

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { key ->
        when (val value = this[key]) {
            is JsonPrimitive -> value.booleanOrNull ?: value.contentOrNull?.toBooleanStrictOrNull()
            else -> null
        }
    }

private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.arrayValue(key: String): JsonArray? = this[key] as? JsonArray
