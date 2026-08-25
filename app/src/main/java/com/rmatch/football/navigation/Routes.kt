package com.rmatch.football.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val MATCHES = "matches"
    const val LEAGUES = "leagues"
    const val ANALYTICS = "analytics"
    const val BROADCAST = "broadcast"
    const val SETTINGS = "settings"

    const val MATCH_DETAIL = "match/{fixtureId}"
    const val TEAM_DETAIL = "team/{teamId}"
    const val PLAYER_DETAIL = "player/{playerId}"
    const val LEAGUE_DETAIL = "league/{leagueId}/{season}"

    fun matchDetail(fixtureId: Int) = "match/$fixtureId"
    fun teamDetail(teamId: Int) = "team/$teamId"
    fun playerDetail(playerId: Int) = "player/$playerId"
    fun leagueDetail(leagueId: Int, season: Int) = "league/$leagueId/$season"

    val topLevel = listOf(MATCHES, LEAGUES, ANALYTICS, BROADCAST, SETTINGS)
}
