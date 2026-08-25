package com.rogermichin.rmatch.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class FootballRepository(
    private val service: ApiFootballService,
    private val moshi: Moshi,
    private val cacheDao: CacheDao,
    private val apiKeyStore: ApiKeyStore,
    private val preferenceStore: PreferenceStore,
    private val quotaTracker: QuotaTracker,
    private val analystEngine: AnalystEngine,
) {
    val apiKeyFlow: StateFlow<String?> = apiKeyStore.apiKeyFlow
    val quotaFlow: StateFlow<QuotaInfo> = quotaTracker.quota
    val countryFilter: Flow<String> = preferenceStore.countryFilter
    val leagueFilter: Flow<String> = preferenceStore.leagueFilter
    private val sourceName = "API-Football.com / API-Sports"
    private val requestLocks = ConcurrentHashMap<String, Mutex>()
    private val currentSeason get() = Instant.now().atZone(ZoneId.systemDefault()).year

    fun maskApiKey(): String = apiKeyStore.mask()
    suspend fun saveCountryFilter(value: String) = preferenceStore.saveCountryFilter(value)
    suspend fun saveLeagueFilter(value: String) = preferenceStore.saveLeagueFilter(value)

    suspend fun verifyAndSaveApiKey(candidate: String): Result<ApiHealth> {
        val trimmed = candidate.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Введите API key"))
        return runCatching {
            val response = service.getStatusWithKey(trimmed)
            quotaTracker.updateFromHeaders(response.headers())
            val body = response.requireBody()
            val status = body.response ?: throw IllegalStateException("Нет верифицированных данных")
            status.toDomainApiHealth().also { apiKeyStore.save(trimmed) }
        }
    }

    suspend fun deleteApiKeyAndCache() {
        apiKeyStore.delete()
        cacheDao.clearAll()
    }

    suspend fun clearCache() = cacheDao.clearAll()

    suspend fun getApiHealth(forceRefresh: Boolean = false): DataResource<ApiHealth> = cached("status", Duration.ofMinutes(5), forceRefresh) {
        service.getStatus().requireBody().response?.toDomainApiHealth() ?: throw IllegalStateException("Нет верифицированных данных")
    }

    suspend fun getCountries(forceRefresh: Boolean = false): DataResource<List<Country>> = cached("countries", Duration.ofHours(24), forceRefresh) {
        service.getCountries().requireBody().response.orEmpty().mapNotNull { it.toDomain() }.sortedBy { it.name }
    }

    suspend fun getSeasons(forceRefresh: Boolean = false): DataResource<List<Int>> = cached("seasons", Duration.ofHours(24), forceRefresh) {
        service.getSeasons().requireBody().response.orEmpty().sortedDescending()
    }

    suspend fun getLeagues(country: String? = null, season: Int = currentSeason, forceRefresh: Boolean = false): DataResource<List<LeagueSummary>> =
        cached("leagues:${country.orEmpty()}:$season", Duration.ofHours(12), forceRefresh) {
            service.getLeagues(country = country?.takeIf { it.isNotBlank() }, season = season).requireBody().response.orEmpty()
                .mapNotNull { it.toDomainLeague() }.sortedWith(compareBy<LeagueSummary> { it.country }.thenBy { it.name })
        }

    suspend fun getMatches(next: Int = 20, forceRefresh: Boolean = false): DataResource<List<MatchSummary>> = cached("matches:next:$next", Duration.ofMinutes(15), forceRefresh) {
        service.getFixtures(next = next).requireBody().response.orEmpty().mapNotNull { it.toDomainMatch() }
    }

    suspend fun getLeagueDetails(leagueId: Int, season: Int, forceRefresh: Boolean = false): DataResource<LeagueDetails> {
        val league = getLeagues(season = season, forceRefresh = forceRefresh).data.firstOrNull { it.id == leagueId }
            ?: LeagueSummary(leagueId, "Лига #$leagueId", "", "League", null, season, true, false)
        val standings = cached("standings:$leagueId:$season", Duration.ofMinutes(20), forceRefresh) {
            service.getStandings(leagueId, season).requireBody().response.orEmpty().firstOrNull()?.league?.standings.orEmpty().flatten().mapNotNull { it.toDomainStanding() }
        }
        val fixtures = cached("league-fixtures:$leagueId:$season", Duration.ofMinutes(15), forceRefresh) {
            service.getFixtures(league = leagueId, season = season, next = 15).requireBody().response.orEmpty().mapNotNull { it.toDomainMatch() }
        }
        return DataResource(LeagueDetails(league, standings.data, fixtures.data), latestMeta(standings.meta, fixtures.meta))
    }

    suspend fun getTeamProfile(teamId: Int, leagueId: Int, season: Int, forceRefresh: Boolean = false): DataResource<TeamProfile> {
        val teamInfo = cached<TeamResponseDto?>("team:$teamId:$leagueId:$season", Duration.ofHours(6), forceRefresh) {
            service.getTeams(teamId = teamId, leagueId = leagueId, season = season).requireBody().response.orEmpty().firstOrNull()
        }.data
        val stats = cached("team-stats:$teamId:$leagueId:$season", Duration.ofHours(1), forceRefresh) {
            service.getTeamStatistics(teamId, leagueId, season).requireBody().response?.toDomainTeamStatistics()
        }
        val squad = cached("squad:$teamId", Duration.ofHours(12), forceRefresh) {
            service.getSquad(teamId).requireBody().response.orEmpty().firstOrNull()?.players.orEmpty().map { it.toDomainSquadPlayer() }
        }
        val players = cached("players:$teamId:$season", Duration.ofHours(6), forceRefresh) {
            service.getPlayers(teamId, season).requireBody().response.orEmpty().mapNotNull { it.toDomainPlayerCard() }
        }
        val coaches = cached("coachs:$teamId", Duration.ofHours(12), forceRefresh) {
            service.getCoaches(teamId).requireBody().response.orEmpty().mapNotNull { it.toDomainCoachCard() }
        }
        val recentMatches = cached("team-last:$teamId:$season", Duration.ofMinutes(20), forceRefresh) {
            service.getFixtures(team = teamId, season = season, last = 5).requireBody().response.orEmpty().mapNotNull { it.toDomainMatch() }
        }
        val injuries = cached("injuries:$teamId:$leagueId:$season", Duration.ofMinutes(30), forceRefresh) {
            service.getInjuries(teamId = teamId, leagueId = leagueId, season = season).requireBody().response.orEmpty().mapNotNull { it.toDomainInjuryCard() }
        }
        val mapped = players.data.associateBy { it.id }
        val mergedPlayers = squad.data.map { base -> base.copy(
            nationality = mapped[base.id]?.nationality,
            appearances = mapped[base.id]?.appearances,
            minutes = mapped[base.id]?.minutes,
            goals = mapped[base.id]?.goals,
            assists = mapped[base.id]?.assists,
            yellowCards = mapped[base.id]?.yellowCards,
            redCards = mapped[base.id]?.redCards,
            injured = mapped[base.id]?.injured,
        ) }
        val teamDto = teamInfo?.team
        return DataResource(
            TeamProfile(
                team = TeamSummary(teamId, teamDto?.name ?: "Команда #$teamId", teamDto?.logo),
                country = teamDto?.country,
                founded = teamDto?.founded,
                venue = teamInfo?.venue?.name,
                city = teamInfo?.venue?.city,
                coachs = coaches.data,
                squad = mergedPlayers,
                statistics = stats.data,
                recentMatches = recentMatches.data,
                injuries = injuries.data,
            ),
            latestMeta(stats.meta, recentMatches.meta)
        )
    }

    suspend fun getMatchDetails(fixtureId: Int, leagueId: Int, season: Int, forceRefresh: Boolean = false): DataResource<MatchDetails> {
        val fixture = cached("fixture:$fixtureId", Duration.ofMinutes(10), forceRefresh) {
            service.getFixtures(fixtureId = fixtureId).requireBody().response.orEmpty().firstOrNull()?.toDomainMatch() ?: throw IllegalStateException("Матч не найден")
        }
        val standings = cached("fixture-standings:$leagueId:$season", Duration.ofMinutes(20), forceRefresh) {
            service.getStandings(leagueId, season).requireBody().response.orEmpty().firstOrNull()?.league?.standings.orEmpty().flatten().mapNotNull { it.toDomainStanding() }
        }
        val events = cached("events:$fixtureId", Duration.ofMinutes(5), forceRefresh) {
            service.getFixtureEvents(fixtureId).requireBody().response.orEmpty().mapNotNull { it.toDomainEvent() }
        }
        val statistics = cached("stats:$fixtureId", Duration.ofMinutes(5), forceRefresh) {
            service.getFixtureStatistics(fixtureId).requireBody().response.orEmpty().toDomainStatsTable()
        }
        val lineups = cached("lineups:$fixtureId", Duration.ofMinutes(5), forceRefresh) {
            service.getFixtureLineups(fixtureId).requireBody().response.orEmpty().mapNotNull { it.toDomainLineup() }
        }
        val injuries = cached("fixture-injuries:$fixtureId", Duration.ofMinutes(20), forceRefresh) {
            service.getInjuries(fixtureId = fixtureId, leagueId = leagueId, season = season).requireBody().response.orEmpty().mapNotNull { it.toDomainInjuryCard() }
        }
        val odds = cached("odds:$fixtureId", Duration.ofMinutes(10), forceRefresh) {
            service.getOdds(fixtureId).requireBody().response.orEmpty().flatMap { it.toDomainOddsMarkets() }
        }
        val homeForm = cached("home-form:${fixture.data.homeTeam.id}:$season", Duration.ofMinutes(20), forceRefresh) {
            service.getFixtures(team = fixture.data.homeTeam.id, season = season, last = 10).requireBody().response.orEmpty().mapNotNull { it.toDomainMatch() }
        }
        val awayForm = cached("away-form:${fixture.data.awayTeam.id}:$season", Duration.ofMinutes(20), forceRefresh) {
            service.getFixtures(team = fixture.data.awayTeam.id, season = season, last = 10).requireBody().response.orEmpty().mapNotNull { it.toDomainMatch() }
        }
        val analysis = analystEngine.analyze(
            fixtureId,
            fixture.data.homeTeam,
            fixture.data.awayTeam,
            homeForm.data,
            awayForm.data,
            standings.data,
            injuries.data,
            lineups.data.any { it.starting.isNotEmpty() },
            odds.data,
        ).getOrNull()
        return DataResource(
            MatchDetails(fixture.data, standings.data, homeForm.data, awayForm.data, events.data, statistics.data, lineups.data, injuries.data, odds.data, analysis),
            latestMeta(fixture.meta, events.meta)
        )
    }

    suspend fun getAnalyticsCards(forceRefresh: Boolean = false): DataResource<List<Pair<MatchSummary, MatchAnalysis?>>> {
        val matches = getMatches(next = 10, forceRefresh = forceRefresh)
        return DataResource(matches.data.map { match -> match to runCatching { getMatchDetails(match.fixtureId, match.leagueId, match.season, forceRefresh).data.analysis }.getOrNull() }, matches.meta)
    }

    private suspend inline fun <reified T> cached(key: String, ttl: Duration, forceRefresh: Boolean = false, crossinline loader: suspend () -> T): DataResource<T> {
        val lock = requestLocks.getOrPut(key) { Mutex() }
        return lock.withLock {
            val adapter = moshi.adapter<T>()
            val cached = cacheDao.get(key)
            val now = Instant.now().toEpochMilli()
            if (!forceRefresh && cached != null) {
                val fresh = now - cached.fetchedAt <= cached.ttlMillis
                val value = adapter.fromJson(cached.payload)
                if (fresh && value != null) return@withLock DataResource(value, DataMeta(sourceName, cached.fetchedAt, stale = false))
            }
            try {
                val loaded = loader()
                cacheDao.upsert(CachedPayloadEntity(key, adapter.toJson(loaded), now, ttl.toMillis()))
                DataResource(loaded, DataMeta(sourceName, now, stale = false))
            } catch (error: Exception) {
                if (cached != null) {
                    val value = adapter.fromJson(cached.payload)
                    if (value != null) return@withLock DataResource(value, DataMeta("$sourceName / cache", cached.fetchedAt, stale = true))
                }
                throw error.toReadableFailure()
            }
        }
    }

    private fun latestMeta(first: DataMeta, second: DataMeta): DataMeta = if (first.fetchedAtEpochMillis >= second.fetchedAtEpochMillis) first else second
    private fun Throwable.toReadableFailure(): Throwable = if (this is IOException) IllegalStateException("Похоже, соединение недоступно или API временно не отвечает") else this

    private fun <T> Response<ApiEnvelope<T>>.requireBody(): ApiEnvelope<T> {
        if (!isSuccessful) throw IllegalStateException(code().toErrorMessage(errorBody()))
        val payload = body() ?: throw IllegalStateException("Пустой ответ API")
        val errorsText = payload.errors?.toString().orEmpty()
        if (errorsText.isNotBlank() && errorsText != "[]" && errorsText != "{}") throw IllegalStateException(errorsText)
        return payload
    }

    private fun Int.toErrorMessage(errorBody: ResponseBody?): String = when (this) {
        401, 403 -> "Ключ API не прошёл проверку"
        429 -> "Лимит API исчерпан"
        else -> errorBody?.string()?.takeIf { it.isNotBlank() } ?: "Ошибка API: HTTP $this"
    }

    private fun StatusResponseDto.toDomainApiHealth() = ApiHealth(subscription?.plan, subscription?.active == true, requests?.limit_day, requests?.current, Instant.now().toString())
    private fun CountryDto.toDomain(): Country? = if (code != null && name != null) Country(code, name, flag) else null
    private fun LeagueResponseDto.toDomainLeague(): LeagueSummary? {
        val seasonValue = seasons?.firstOrNull { it.current == true } ?: seasons?.maxByOrNull { it.year ?: 0 }
        return league?.id?.let { id ->
            league.name?.let { name ->
                LeagueSummary(id, name, country?.name.orEmpty(), league.type.orEmpty(), league.logo, seasonValue?.year ?: currentSeason, seasonValue?.coverage?.standings == true, seasonValue?.coverage?.odds == true)
            }
        }
    }
    private fun TeamDto.toDomainTeam(): TeamSummary? = if (id != null && name != null) TeamSummary(id, name, logo) else null
    private fun FixtureResponseDto.toDomainMatch(): MatchSummary? {
        val f = fixture ?: return null
        val t = teams ?: return null
        val l = league ?: return null
        return MatchSummary(f.id ?: return null, f.date ?: return null, f.timestamp ?: return null, f.status?.long ?: "Неизвестно", f.status?.short ?: "", l.id ?: return null, l.name ?: "Лига", l.country.orEmpty(), l.round, f.venue?.name, f.referee, t.home?.toDomainTeam() ?: return null, t.away?.toDomainTeam() ?: return null, goals?.home, goals?.away, l.season ?: currentSeason)
    }
    private fun StandingRowDto.toDomainStanding(): StandingRow? = StandingRow(rank ?: return null, team?.toDomainTeam() ?: return null, points ?: 0, goalsDiff ?: 0, form, all?.played ?: 0, all?.win ?: 0, all?.draw ?: 0, all?.lose ?: 0, all?.goals?.`for` ?: 0, all?.goals?.against ?: 0)
    private fun TeamStatisticsDto.toDomainTeamStatistics() = TeamStatistics(form, fixtures?.played?.home, fixtures?.played?.away, goals?.`for`?.average?.home?.toDoubleOrNull(), goals?.`for`?.average?.away?.toDoubleOrNull(), goals?.against?.average?.home?.toDoubleOrNull(), goals?.against?.average?.away?.toDoubleOrNull(), clean_sheet?.total, failed_to_score?.total, biggest?.streak?.wins)
    private fun PlayerResponseDto.toDomainPlayerCard(): PlayerCard? { val p = player ?: return null; val s = statistics?.firstOrNull(); return if (p.id != null && p.name != null) PlayerCard(p.id, p.name, p.age, p.nationality, s?.games?.position, null, s?.games?.appearences, s?.games?.minutes, s?.goals?.total, s?.goals?.assists, s?.cards?.yellow, s?.cards?.red, p.injured) else null }
    private fun SquadPlayerDto.toDomainSquadPlayer() = PlayerCard(id ?: 0, name.orEmpty(), age, null, position, number, null, null, null, null, null, null, null)
    private fun CoachResponseDto.toDomainCoachCard(): CoachCard? = if (id != null && name != null) CoachCard(id, name, nationality, age, team?.name) else null
    private fun EventDto.toDomainEvent(): MatchEvent? = MatchEvent(buildString { append(time?.elapsed ?: return null); time?.extra?.let { append("+$it") }; append("′") }, team?.name ?: return null, player?.name, listOfNotNull(type, detail, comments).joinToString(" · "), assist?.name)
    private fun List<FixtureStatisticsTeamDto>.toDomainStatsTable(): List<MatchStatisticRow> { if (size < 2) return emptyList(); val home = firstOrNull()?.statistics.orEmpty().associateBy { it.type }; val away = getOrNull(1)?.statistics.orEmpty().associateBy { it.type }; return home.keys.sorted().mapNotNull { key -> key?.let { MatchStatisticRow(it, home[it]?.value?.toString() ?: "—", away[it]?.value?.toString() ?: "—") } } }
    private fun LineupDto.toDomainLineup(): MatchLineup? = MatchLineup(team?.toDomainTeam() ?: return null, coach?.let { CoachCard(it.id ?: 0, it.name.orEmpty(), null, null, team?.name) }, formation, startXI.orEmpty().mapNotNull { it.player?.toDomainLineupPlayer() }, substitutes.orEmpty().mapNotNull { it.player?.toDomainLineupPlayer() })
    private fun LineupPlayerDto.toDomainLineupPlayer(): LineupPlayer? = if (id != null && name != null) LineupPlayer(id, name, number, pos, grid) else null
    private fun InjuryResponseDto.toDomainInjuryCard(): InjuryCard? = if (player?.name != null && team?.name != null) InjuryCard(player.name, team.name, injury?.type, injury?.reason) else null
    private fun OddsResponseDto.toDomainOddsMarkets(): List<OddsMarket> = bookmakers.orEmpty().flatMap { bookmaker -> bookmaker.bets.orEmpty().mapNotNull { bet ->
        val values = bet.values.orEmpty().mapNotNull { value -> value.odd?.toDoubleOrNull()?.takeIf { it > 1.0 }?.let { OddValue(value.value ?: return@mapNotNull null, it, analystEngine.impliedProbability(it)) } }
        if (values.isEmpty()) null else OddsMarket(bookmaker.name ?: "Bookmaker", bet.name ?: "Market", values, update, update?.let { runCatching { Duration.between(Instant.parse(it), Instant.now()).toHours() <= 24 }.getOrDefault(false) } ?: false)
    } }
}
