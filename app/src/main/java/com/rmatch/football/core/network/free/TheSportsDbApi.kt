package com.rmatch.football.core.network.free

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheSportsDB v1 API (completely free, no key required for public endpoints).
 * Base URL: https://www.thesportsdb.com/api/v1/json/3/
 *
 * Used as a fallback when the paid API-Football key is absent, exhausted, or returns no data.
 */
interface TheSportsDbApi {

    /** Events for a given date, optionally filtered by sport. */
    @GET("eventsday.php")
    suspend fun eventsByDay(
        @Query("d") date: String,
        @Query("s") sport: String = "Soccer"
    ): Response<TheSportsDbEventsEnvelope>

    /** Look up a single event by its TheSportsDB event ID. */
    @GET("lookupevent.php")
    suspend fun eventById(
        @Query("id") id: Int
    ): Response<TheSportsDbEventsEnvelope>
}
