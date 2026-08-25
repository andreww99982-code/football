package com.rmatch.football.core.network

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
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.QueryMap

/** Official API-Football (API-Sports) v3 endpoints. No scraping, no third party mirrors. */
interface FootballApi {

    @GET("status")
    suspend fun status(
        @Header(ApiConstants.HEADER_KEY) overrideKey: String? = null
    ): Response<ApiEnvelope<StatusDto>>

    @GET("countries")
    suspend fun countries(): Response<ApiEnvelope<List<CountryDto>>>

    @GET("leagues")
    suspend fun leagues(
        @Query("season") season: Int? = null,
        @Query("country") country: String? = null
    ): Response<ApiEnvelope<List<LeagueDto>>>

    @GET("fixtures")
    suspend fun fixtures(
        @QueryMap params: Map<String, String>
    ): Response<ApiEnvelope<List<FixtureDto>>>

    @GET("fixtures/events")
    suspend fun fixtureEvents(
        @Query("fixture") fixtureId: Int
    ): Response<ApiEnvelope<List<FixtureEventDto>>>

    @GET("fixtures/statistics")
    suspend fun fixtureStatistics(
        @Query("fixture") fixtureId: Int
    ): Response<ApiEnvelope<List<TeamStatisticsBlockDto>>>

    @GET("fixtures/lineups")
    suspend fun fixtureLineups(
        @Query("fixture") fixtureId: Int
    ): Response<ApiEnvelope<List<LineupDto>>>

    @GET("standings")
    suspend fun standings(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): Response<ApiEnvelope<List<StandingsResponseDto>>>

    @GET("teams")
    suspend fun teams(
        @Query("id") teamId: Int
    ): Response<ApiEnvelope<List<TeamProfileDto>>>

    @GET("teams/statistics")
    suspend fun teamStatistics(
        @Query("team") teamId: Int,
        @Query("season") season: Int,
        @Query("league") leagueId: Int
    ): Response<ApiEnvelope<TeamStatisticsDto>>

    @GET("players")
    suspend fun players(
        @Query("team") teamId: Int? = null,
        @Query("id") playerId: Int? = null,
        @Query("season") season: Int? = null,
        @Query("page") page: Int? = null
    ): Response<ApiEnvelope<List<PlayerResponseDto>>>

    @GET("players/squads")
    suspend fun squads(
        @Query("team") teamId: Int
    ): Response<ApiEnvelope<List<SquadDto>>>

    @GET("coachs")
    suspend fun coaches(
        @Query("team") teamId: Int
    ): Response<ApiEnvelope<List<CoachDto>>>

    @GET("injuries")
    suspend fun injuries(
        @Query("fixture") fixtureId: Int
    ): Response<ApiEnvelope<List<InjuryDto>>>

    @GET("odds")
    suspend fun odds(
        @Query("fixture") fixtureId: Int
    ): Response<ApiEnvelope<List<OddsDto>>>
}
