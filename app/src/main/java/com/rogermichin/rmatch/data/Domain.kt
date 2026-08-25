package com.rogermichin.rmatch.data

data class Country(val code: String, val name: String, val flagUrl: String?)
data class LeagueSummary(
    val id: Int,
    val name: String,
    val country: String,
    val type: String,
    val logoUrl: String?,
    val season: Int,
    val standingsSupported: Boolean,
    val oddsSupported: Boolean,
)
data class TeamSummary(val id: Int, val name: String, val logoUrl: String? = null)
data class MatchSummary(
    val fixtureId: Int,
    val dateIso: String,
    val timestamp: Long,
    val status: String,
    val statusShort: String,
    val leagueId: Int,
    val leagueName: String,
    val country: String,
    val round: String?,
    val venue: String?,
    val referee: String?,
    val homeTeam: TeamSummary,
    val awayTeam: TeamSummary,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val season: Int,
)
data class StandingRow(
    val rank: Int,
    val team: TeamSummary,
    val points: Int,
    val goalDifference: Int,
    val form: String?,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
)
data class LeagueDetails(val league: LeagueSummary, val standings: List<StandingRow>, val fixtures: List<MatchSummary>)
data class PlayerCard(
    val id: Int,
    val name: String,
    val age: Int?,
    val nationality: String?,
    val position: String?,
    val number: Int?,
    val appearances: Int?,
    val minutes: Int?,
    val goals: Int?,
    val assists: Int?,
    val yellowCards: Int?,
    val redCards: Int?,
    val injured: Boolean?,
)
data class CoachCard(val id: Int, val name: String, val nationality: String?, val age: Int?, val currentTeam: String?)
data class InjuryCard(val playerName: String, val teamId: Int, val teamName: String, val type: String?, val reason: String?)
data class TeamStatistics(
    val form: String?,
    val playedHome: Int?,
    val playedAway: Int?,
    val goalsForHomeAvg: Double?,
    val goalsForAwayAvg: Double?,
    val goalsAgainstHomeAvg: Double?,
    val goalsAgainstAwayAvg: Double?,
    val cleanSheets: Int?,
    val failedToScore: Int?,
    val streakWins: Int?,
)
data class TeamProfile(
    val team: TeamSummary,
    val country: String?,
    val founded: Int?,
    val venue: String?,
    val city: String?,
    val coaches: List<CoachCard>,
    val squad: List<PlayerCard>,
    val statistics: TeamStatistics?,
    val recentMatches: List<MatchSummary>,
    val injuries: List<InjuryCard>,
)
data class MatchEvent(val minute: String, val teamName: String, val playerName: String?, val detail: String, val assistName: String?)
data class MatchStatisticRow(val label: String, val homeValue: String, val awayValue: String)
data class LineupPlayer(val id: Int, val name: String, val number: Int?, val position: String?, val grid: String?)
data class MatchLineup(val team: TeamSummary, val coach: CoachCard?, val formation: String?, val starting: List<LineupPlayer>, val bench: List<LineupPlayer>)
data class OddValue(val label: String, val decimal: Double, val impliedProbability: Double)
data class OddsMarket(val bookmaker: String, val market: String, val values: List<OddValue>, val updatedAtIso: String?, val isFresh: Boolean)
data class MarketComparison(
    val bookmaker: String,
    val market: String,
    val label: String,
    val modelProbability: Double,
    val normalizedMarketProbability: Double,
    val delta: Double,
)
data class AnalystFactor(val label: String, val impact: String)
data class AnalystOutcome(val label: String, val probability: Double)
data class MatchAnalysis(
    val fixtureId: Int,
    val usedMatches: List<Int>,
    val modelName: String,
    val dataQuality: String,
    val calculatedAtIso: String,
    val expectedHomeGoals: Double,
    val expectedAwayGoals: Double,
    val oneXTwo: List<AnalystOutcome>,
    val totals: List<AnalystOutcome>,
    val bothTeamsToScore: List<AnalystOutcome>,
    val handicaps: List<AnalystOutcome>,
    val factors: List<AnalystFactor>,
    val disclaimer: String,
    val marketComparisons: List<MarketComparison>,
)
data class MatchDetails(
    val summary: MatchSummary,
    val standings: List<StandingRow>,
    val homeForm: List<MatchSummary>,
    val awayForm: List<MatchSummary>,
    val events: List<MatchEvent>,
    val statistics: List<MatchStatisticRow>,
    val lineups: List<MatchLineup>,
    val injuries: List<InjuryCard>,
    val odds: List<OddsMarket>,
    val analysis: MatchAnalysis?,
)
data class ApiHealth(val subscriptionPlan: String?, val active: Boolean, val dailyLimit: Int?, val currentUsage: Int?, val checkedAtIso: String)
data class QuotaInfo(val requestsLimit: Int? = null, val requestsRemaining: Int? = null, val dailyLimit: Int? = null, val usedToday: Int? = null)
data class DataMeta(val source: String, val fetchedAtEpochMillis: Long, val stale: Boolean)
data class DataResource<T>(val data: T, val meta: DataMeta)
data class ScreenData<T>(val value: T? = null, val meta: DataMeta? = null, val loading: Boolean = false, val error: String? = null, val emptyMessage: String? = null)
