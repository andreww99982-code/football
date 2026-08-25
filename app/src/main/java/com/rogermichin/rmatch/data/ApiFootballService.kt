package com.rogermichin.rmatch.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiFootballService {
    @GET("status")
    suspend fun getStatus(): Response<ApiEnvelope<StatusResponseDto>>

    @GET("status")
    suspend fun getStatusWithKey(@Header("x-apisports-key") apiKey: String): Response<ApiEnvelope<StatusResponseDto>>

    @GET("countries")
    suspend fun getCountries(): Response<ApiEnvelope<List<CountryDto>>>

    @GET("leagues")
    suspend fun getLeagues(
        @Query("country") country: String? = null,
        @Query("season") season: Int? = null,
        @Query("current") current: Boolean? = null,
    ): Response<ApiEnvelope<List<LeagueResponseDto>>>

    @GET("leagues/seasons")
    suspend fun getSeasons(): Response<ApiEnvelope<List<Int>>>

    @GET("fixtures")
    suspend fun getFixtures(
        @Query("next") next: Int? = null,
        @Query("last") last: Int? = null,
        @Query("league") league: Int? = null,
        @Query("season") season: Int? = null,
        @Query("team") team: Int? = null,
        @Query("id") fixtureId: Int? = null,
    ): Response<ApiEnvelope<List<FixtureResponseDto>>>

    @GET("fixtures/events")
    suspend fun getFixtureEvents(@Query("fixture") fixtureId: Int): Response<ApiEnvelope<List<EventDto>>>

    @GET("fixtures/statistics")
    suspend fun getFixtureStatistics(@Query("fixture") fixtureId: Int): Response<ApiEnvelope<List<FixtureStatisticsTeamDto>>>

    @GET("fixtures/lineups")
    suspend fun getFixtureLineups(@Query("fixture") fixtureId: Int): Response<ApiEnvelope<List<LineupDto>>>

    @GET("standings")
    suspend fun getStandings(@Query("league") leagueId: Int, @Query("season") season: Int): Response<ApiEnvelope<List<StandingsResponseDto>>>

    @GET("teams")
    suspend fun getTeams(
        @Query("id") teamId: Int? = null,
        @Query("league") leagueId: Int? = null,
        @Query("season") season: Int? = null,
    ): Response<ApiEnvelope<List<TeamResponseDto>>>

    @GET("teams/statistics")
    suspend fun getTeamStatistics(@Query("team") teamId: Int, @Query("league") leagueId: Int, @Query("season") season: Int): Response<ApiEnvelope<TeamStatisticsDto>>

    @GET("players")
    suspend fun getPlayers(@Query("team") teamId: Int, @Query("season") season: Int, @Query("page") page: Int = 1): Response<ApiEnvelope<List<PlayerResponseDto>>>

    @GET("players/squads")
    suspend fun getSquad(@Query("team") teamId: Int): Response<ApiEnvelope<List<SquadResponseDto>>>

    @GET("coachs")
    suspend fun getCoaches(@Query("team") teamId: Int): Response<ApiEnvelope<List<CoachResponseDto>>>

    @GET("injuries")
    suspend fun getInjuries(
        @Query("team") teamId: Int? = null,
        @Query("league") leagueId: Int? = null,
        @Query("season") season: Int? = null,
        @Query("fixture") fixtureId: Int? = null,
    ): Response<ApiEnvelope<List<InjuryResponseDto>>>

    @GET("odds")
    suspend fun getOdds(@Query("fixture") fixtureId: Int): Response<ApiEnvelope<List<OddsResponseDto>>>
}
