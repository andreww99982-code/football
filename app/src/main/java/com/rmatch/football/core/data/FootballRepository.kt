package com.rmatch.football.core.data

import com.rmatch.football.core.db.ApiCacheDao
import com.rmatch.football.core.db.ApiCacheEntity
import com.rmatch.football.core.domain.model.Coach
import com.rmatch.football.core.domain.model.Country
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.Injury
import com.rmatch.football.core.domain.model.LeagueSummary
import com.rmatch.football.core.domain.model.Lineup
import com.rmatch.football.core.domain.model.MatchEvent
import com.rmatch.football.core.domain.model.OddsBoard
import com.rmatch.football.core.domain.model.PlayerProfile
import com.rmatch.football.core.domain.model.ProviderStatus
import com.rmatch.football.core.domain.model.SquadMember
import com.rmatch.football.core.domain.model.StandingRow
import com.rmatch.football.core.domain.model.TeamMatchStatistics
import com.rmatch.football.core.domain.model.TeamProfile
import com.rmatch.football.core.domain.model.TeamSeasonStats
import com.rmatch.football.core.network.ApiConstants
import com.rmatch.football.core.network.FootballApi
import com.rmatch.football.core.network.QuotaTracker
import com.rmatch.football.core.network.free.FreeFootballDataSource
import com.rmatch.football.core.network.free.OpenLigaDbDataSource
import com.rmatch.football.core.network.dto.ApiEnvelope
import com.rmatch.football.core.network.dto.CoachDto
import com.rmatch.football.core.network.dto.CountryDto
import com.rmatch.football.core.network.dto.FixtureDto
import com.rmatch.football.core.network.dto.FixtureEventDto
import com.rmatch.football.core.network.dto.InjuryDto
import com.rmatch.football.core.network.dto.LeagueDto
import com.rmatch.football.core.network.dto.LineupDto
import com.rmatch.football.core.network.dto.OddsDto
import com.rmatch.football.core.network.dto.PlayerResponseDto
import com.rmatch.football.core.network.dto.SquadDto
import com.rmatch.football.core.network.dto.StandingsResponseDto
import com.rmatch.football.core.network.dto.StatusDto
import com.rmatch.football.core.network.dto.TeamProfileDto
import com.rmatch.football.core.network.dto.TeamStatisticsBlockDto
import com.rmatch.football.core.network.dto.TeamStatisticsDto
import com.rmatch.football.core.network.mapper.toDomain
import com.rmatch.football.core.network.mapper.toDomainOrNull
import com.rmatch.football.core.network.mapper.toFixtures
import com.rmatch.football.core.network.mapper.toMembers
import com.rmatch.football.core.network.mapper.toStandingRows
import com.rmatch.football.core.security.ApiKeyStorage
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.Loaded
import com.rmatch.football.core.util.map
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response

/**
 * Single source of truth for provider data.
 * Only official API-Football endpoints are used, results are cached with a TTL
 * and stale cache is served (clearly marked) when the network fails.
 */
class FootballRepository(
    private val api: FootballApi,
    private val cacheDao: ApiCacheDao,
    private val json: Json,
    private val keyStorage: ApiKeyStorage,
    private val quotaTracker: QuotaTracker,
    private val freeDataSource: FreeFootballDataSource? = null,
    private val openLigaDbDataSource: OpenLigaDbDataSource? = null,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    val quota = quotaTracker.quota

    val attribution: String = ApiConstants.ATTRIBUTION

    fun hasApiKey(): Boolean = keyStorage.hasKey()

    suspend fun validateAndSaveKey(candidate: String): DataResult<ProviderStatus> =
        withContext(Dispatchers.IO) {
            val trimmed = candidate.trim()
            try {
                val response = api.status(trimmed)
                when {
                    response.code() == 401 || response.code() == 403 ->
                        DataResult.Failure(AppError.Unauthorized)

                    response.code() == 429 -> DataResult.Failure(AppError.RateLimited)
                    !response.isSuccessful -> DataResult.Failure(AppError.Http(response.code()))
                    else -> {
                        val envelope = response.body()
                        val error = envelope?.errors.errorMessage()
                        val payload = envelope?.response
                        when {
                            error != null && error.containsKeyProblem() ->
                                DataResult.Failure(AppError.Unauthorized)

                            error != null -> DataResult.Failure(AppError.Api(error))
                            payload == null -> DataResult.Failure(AppError.Unauthorized)
                            else -> {
                                keyStorage.saveKey(trimmed)
                                DataResult.Success(
                                    Loaded(payload.toProviderStatus(), nowProvider(), false)
                                )
                            }
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                DataResult.Failure(AppError.Network(io.message ?: "нет соединения"))
            } catch (error: Exception) {
                DataResult.Failure(AppError.Api(error.message ?: "неизвестная ошибка"))
            }
        }

    suspend fun clearKeyAndCache() = withContext(Dispatchers.IO) {
        keyStorage.clearKey()
        runCatching { cacheDao.clear() }
        quotaTracker.reset()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        runCatching { cacheDao.clear() }
        Unit
    }

    suspend fun cachedEntries(): Int = withContext(Dispatchers.IO) {
        runCatching { cacheDao.count() }.getOrDefault(0)
    }

    suspend fun lastCacheUpdate(): Long? = withContext(Dispatchers.IO) {
        runCatching { cacheDao.lastUpdate() }.getOrNull()
    }

    suspend fun providerStatus(forceRefresh: Boolean = true): DataResult<ProviderStatus> =
        load(
            cacheKey = "status",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = StatusDto.serializer(),
            forceRefresh = forceRefresh
        ) { api.status(null) }.map { it.toProviderStatus() }

    suspend fun countries(forceRefresh: Boolean = false): DataResult<List<Country>> =
        load(
            cacheKey = "countries",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(CountryDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.countries() }.map { list -> list.mapNotNull { it.toDomainOrNull() } }

    suspend fun leagues(season: Int, forceRefresh: Boolean = false): DataResult<List<LeagueSummary>> {
        val paid = load(
            cacheKey = "leagues:$season",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(LeagueDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.leagues(season = season) }
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }
        if (shouldUseFreeFallback(paid) && openLigaDbDataSource != null) {
            val free = openLigaDbDataSource.leagues(season)
            if (free is DataResult.Success && free.loaded.value.isNotEmpty()) return free
        }
        return paid
    }

    suspend fun fixturesByDate(
        date: LocalDate,
        forceRefresh: Boolean = false
    ): DataResult<List<Fixture>> {
        val paid = load(
            cacheKey = "fixtures:date:$date",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtures(mapOf("date" to date.toString())) }
            .map { it.toFixtures() }

        // Fall back to free source when paid API has no key, quota exceeded, or returned empty
        val needsFallback = shouldUseFreeFallback(paid)
        if (needsFallback && freeDataSource != null) {
            val free = freeDataSource.fixturesByDate(date)
            if (free is DataResult.Success && free.loaded.value.isNotEmpty()) return free
        }
        return paid
    }

    suspend fun liveFixtures(forceRefresh: Boolean = true): DataResult<List<Fixture>> =
        load(
            cacheKey = "fixtures:live",
            ttlMillis = CacheTtl.LIVE_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtures(mapOf("live" to "all")) }
            .map { it.toFixtures() }

    suspend fun nextFixturesForLeague(
        leagueId: Int,
        season: Int,
        next: Int = 10,
        forceRefresh: Boolean = false
    ): DataResult<List<Fixture>> {
        if (openLigaDbDataSource?.canHandleLeague(leagueId) == true) {
            return openLigaDbDataSource.nextFixturesForLeague(leagueId, season, next)
        }
        val paid = load(
            cacheKey = "fixtures:league:$leagueId:$season:next:$next",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) {
            api.fixtures(
                mapOf(
                    "league" to leagueId.toString(),
                    "season" to season.toString(),
                    "next" to next.toString()
                )
            )
        }.map { it.toFixtures() }
        if (shouldUseFreeFallback(paid) && openLigaDbDataSource != null) {
            val free = openLigaDbDataSource.nextFixturesForLeague(leagueId, season, next)
            if (free is DataResult.Success && free.loaded.value.isNotEmpty()) return free
        }
        return paid
    }

    suspend fun lastFixturesForTeam(
        teamId: Int,
        last: Int = 10,
        forceRefresh: Boolean = false
    ): DataResult<List<Fixture>> =
        load(
            cacheKey = "fixtures:team:$teamId:last:$last",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) {
            api.fixtures(mapOf("team" to teamId.toString(), "last" to last.toString()))
        }.map { it.toFixtures() }

    suspend fun nextFixturesForTeam(
        teamId: Int,
        next: Int = 5,
        forceRefresh: Boolean = false
    ): DataResult<List<Fixture>> =
        load(
            cacheKey = "fixtures:team:$teamId:next:$next",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) {
            api.fixtures(mapOf("team" to teamId.toString(), "next" to next.toString()))
        }.map { it.toFixtures() }

    suspend fun fixture(fixtureId: Int, forceRefresh: Boolean = false): DataResult<Fixture?> {
        if (openLigaDbDataSource?.canHandleFixture(fixtureId) == true) {
            return openLigaDbDataSource.fixtureById(fixtureId)
        }
        val paid = load(
            cacheKey = "fixture:$fixtureId",
            ttlMillis = CacheTtl.LIVE_MILLIS,
            serializer = ListSerializer(FixtureDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtures(mapOf("id" to fixtureId.toString())) }
            .map { it.toFixtures().firstOrNull() }

        val needsFallback = paid is DataResult.Failure &&
            (paid.error is AppError.NoApiKey ||
                paid.error is AppError.RateLimited ||
                paid.error is AppError.Unauthorized)
        if (needsFallback && freeDataSource != null) {
            val free = freeDataSource.fixtureById(fixtureId)
            if (free is DataResult.Success && free.loaded.value != null) return free
        }
        if (needsFallback && openLigaDbDataSource != null) {
            val free = openLigaDbDataSource.fixtureById(fixtureId)
            if (free is DataResult.Success && free.loaded.value != null) return free
        }
        return paid
    }

    suspend fun events(fixtureId: Int, forceRefresh: Boolean = false): DataResult<List<MatchEvent>> =
        load(
            cacheKey = "events:$fixtureId",
            ttlMillis = CacheTtl.LIVE_MILLIS,
            serializer = ListSerializer(FixtureEventDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtureEvents(fixtureId) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun statistics(
        fixtureId: Int,
        forceRefresh: Boolean = false
    ): DataResult<List<TeamMatchStatistics>> =
        load(
            cacheKey = "statistics:$fixtureId",
            ttlMillis = CacheTtl.LIVE_MILLIS,
            serializer = ListSerializer(TeamStatisticsBlockDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtureStatistics(fixtureId) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun lineups(fixtureId: Int, forceRefresh: Boolean = false): DataResult<List<Lineup>> =
        load(
            cacheKey = "lineups:$fixtureId",
            ttlMillis = CacheTtl.LIVE_MILLIS,
            serializer = ListSerializer(LineupDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.fixtureLineups(fixtureId) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun standings(
        leagueId: Int,
        season: Int,
        forceRefresh: Boolean = false
    ): DataResult<List<StandingRow>> {
        if (openLigaDbDataSource?.canHandleLeague(leagueId) == true) {
            return openLigaDbDataSource.standings(leagueId, season)
        }
        val paid = load(
            cacheKey = "standings:$leagueId:$season",
            ttlMillis = CacheTtl.STANDINGS_MILLIS,
            serializer = ListSerializer(StandingsResponseDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.standings(leagueId, season) }
            .map { it.toStandingRows() }
        if (shouldUseFreeFallback(paid) && openLigaDbDataSource != null) {
            val free = openLigaDbDataSource.standings(leagueId, season)
            if (free is DataResult.Success && free.loaded.value.isNotEmpty()) return free
        }
        return paid
    }

    suspend fun teamProfile(teamId: Int, forceRefresh: Boolean = false): DataResult<TeamProfile?> =
        load(
            cacheKey = "team:$teamId",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(TeamProfileDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.teams(teamId) }
            .map { list -> list.firstNotNullOfOrNull { it.toDomainOrNull() } }

    suspend fun teamStatistics(
        teamId: Int,
        season: Int,
        leagueId: Int,
        forceRefresh: Boolean = false
    ): DataResult<TeamSeasonStats> =
        load(
            cacheKey = "teamstats:$teamId:$season:$leagueId",
            ttlMillis = CacheTtl.STANDINGS_MILLIS,
            serializer = TeamStatisticsDto.serializer(),
            forceRefresh = forceRefresh
        ) { api.teamStatistics(teamId, season, leagueId) }
            .map { it.toDomain() }

    suspend fun squad(teamId: Int, forceRefresh: Boolean = false): DataResult<List<SquadMember>> =
        load(
            cacheKey = "squad:$teamId",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(SquadDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.squads(teamId) }
            .map { list -> list.flatMap { it.toMembers() } }

    suspend fun coaches(teamId: Int, forceRefresh: Boolean = false): DataResult<List<Coach>> =
        load(
            cacheKey = "coaches:$teamId",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(CoachDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.coaches(teamId) }
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }

    suspend fun injuries(fixtureId: Int, forceRefresh: Boolean = false): DataResult<List<Injury>> =
        load(
            cacheKey = "injuries:$fixtureId",
            ttlMillis = CacheTtl.UPCOMING_MILLIS,
            serializer = ListSerializer(InjuryDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.injuries(fixtureId) }
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }

    suspend fun odds(fixtureId: Int, forceRefresh: Boolean = false): DataResult<List<OddsBoard>> =
        load(
            cacheKey = "odds:$fixtureId",
            ttlMillis = CacheTtl.ODDS_MILLIS,
            serializer = ListSerializer(OddsDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.odds(fixtureId) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun players(
        teamId: Int? = null,
        playerId: Int? = null,
        season: Int? = null,
        forceRefresh: Boolean = false
    ): DataResult<List<PlayerProfile>> =
        load(
            cacheKey = "players:${teamId ?: 0}:${playerId ?: 0}:${season ?: 0}",
            ttlMillis = CacheTtl.STATIC_MILLIS,
            serializer = ListSerializer(PlayerResponseDto.serializer()),
            forceRefresh = forceRefresh
        ) { api.players(teamId = teamId, playerId = playerId, season = season) }
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }

    private suspend fun <D : Any> load(
        cacheKey: String,
        ttlMillis: Long,
        serializer: KSerializer<D>,
        forceRefresh: Boolean,
        call: suspend () -> Response<ApiEnvelope<D>>
    ): DataResult<D> = withContext(Dispatchers.IO) {
        if (!keyStorage.hasKey()) {
            return@withContext DataResult.Failure(AppError.NoApiKey)
        }
        val now = nowProvider()
        val cached = readCache(cacheKey, serializer)
        if (!forceRefresh && cached != null && now - cached.fetchedAtMillis < ttlMillis) {
            return@withContext DataResult.Success(cached)
        }

        try {
            val response = call()
            if (!response.isSuccessful) {
                val error = when (response.code()) {
                    401, 403 -> AppError.Unauthorized
                    429 -> AppError.RateLimited
                    else -> AppError.Http(response.code())
                }
                return@withContext cached?.let { DataResult.Success(it) }
                    ?: DataResult.Failure(error)
            }
            val envelope = response.body()
                ?: return@withContext cached?.let { DataResult.Success(it) }
                    ?: DataResult.Failure(AppError.EmptyResponse)

            val apiError = envelope.errors.errorMessage()
            if (apiError != null) {
                val error = when {
                    apiError.containsKeyProblem() -> AppError.Unauthorized
                    apiError.containsQuotaProblem() -> AppError.RateLimited
                    else -> AppError.Api(apiError)
                }
                return@withContext cached?.let { DataResult.Success(it) }
                    ?: DataResult.Failure(error)
            }

            val payload = envelope.response
                ?: return@withContext cached?.let { DataResult.Success(it) }
                    ?: DataResult.Failure(AppError.EmptyResponse)

            writeCache(cacheKey, serializer, payload, now)
            DataResult.Success(Loaded(payload, now, fromCache = false))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            cached?.let { DataResult.Success(it) }
                ?: DataResult.Failure(AppError.Network(io.message ?: "нет соединения"))
        } catch (error: Exception) {
            cached?.let { DataResult.Success(it) }
                ?: DataResult.Failure(AppError.Api(error.message ?: "неизвестная ошибка"))
        }
    }

    private suspend fun <D : Any> readCache(
        cacheKey: String,
        serializer: KSerializer<D>
    ): Loaded<D>? {
        val entity = runCatching { cacheDao.find(cacheKey) }.getOrNull() ?: return null
        val decoded = runCatching { json.decodeFromString(serializer, entity.payload) }.getOrNull()
            ?: return null
        return Loaded(decoded, entity.fetchedAtMillis, fromCache = true)
    }

    private suspend fun <D : Any> writeCache(
        cacheKey: String,
        serializer: KSerializer<D>,
        payload: D,
        nowMillis: Long
    ) {
        runCatching {
            cacheDao.upsert(
                ApiCacheEntity(
                    cacheKey = cacheKey,
                    payload = json.encodeToString(serializer, payload),
                    fetchedAtMillis = nowMillis
                )
            )
        }
    }
}

private fun StatusDto.toProviderStatus(): ProviderStatus = ProviderStatus(
    plan = subscription?.plan,
    subscriptionEnd = subscription?.end,
    active = subscription?.active,
    requestsToday = requests?.current,
    requestsLimitPerDay = requests?.limitDay
)

private fun String.containsKeyProblem(): Boolean {
    val lower = lowercase()
    return lower.contains("token") || lower.contains("key") || lower.contains("subscription")
}

private fun String.containsQuotaProblem(): Boolean {
    val lower = lowercase()
    return lower.contains("rate") || lower.contains("limit") || lower.contains("quota")
}

private fun JsonElement?.errorMessage(): String? = when (this) {
    null -> null
    is JsonArray -> if (isEmpty()) null else joinToString(separator = "; ") { it.plainText() }
    is JsonObject -> if (isEmpty()) {
        null
    } else {
        entries.joinToString(separator = "; ") { "${it.key}: ${it.value.plainText()}" }
    }
    is JsonPrimitive -> content.takeIf { it.isNotBlank() && it != "null" }
    else -> null
}

private fun JsonElement.plainText(): String =
    if (this is JsonPrimitive) content else toString()

private fun <T> shouldUseFreeFallback(result: DataResult<List<T>>): Boolean = when (result) {
    is DataResult.Failure -> result.error is AppError.NoApiKey ||
        result.error is AppError.RateLimited ||
        result.error is AppError.Unauthorized
    is DataResult.Success -> result.loaded.value.isEmpty()
}
