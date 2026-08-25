package com.rmatch.football.core.data

/** Time-to-live policy per data family (documented in README). */
object CacheTtl {
    const val LIVE_MILLIS = 30_000L
    const val UPCOMING_MILLIS = 5 * 60_000L
    const val ODDS_MILLIS = 10 * 60_000L
    const val STANDINGS_MILLIS = 60 * 60_000L
    const val STATIC_MILLIS = 24 * 60 * 60_000L
}
