package com.rmatch.football.core.network.free

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenLigaDbApi {

    @GET("getavailableleagues")
    suspend fun availableLeagues(): Response<JsonElement>

    @GET("getmatchdata/{leagueShortcut}/{season}")
    suspend fun matchesByLeague(
        @Path("leagueShortcut") leagueShortcut: String,
        @Path("season") season: Int
    ): Response<JsonElement>

    @GET("getbltable/{leagueShortcut}/{season}")
    suspend fun standings(
        @Path("leagueShortcut") leagueShortcut: String,
        @Path("season") season: Int
    ): Response<JsonElement>

    @GET("getmatchdata/{matchId}")
    suspend fun matchById(
        @Path("matchId") matchId: Int
    ): Response<JsonElement>
}
