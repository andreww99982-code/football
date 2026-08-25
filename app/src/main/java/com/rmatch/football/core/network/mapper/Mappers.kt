package com.rmatch.football.core.network.mapper

import com.rmatch.football.core.domain.model.Coach
import com.rmatch.football.core.domain.model.Country
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.Injury
import com.rmatch.football.core.domain.model.LeagueRef
import com.rmatch.football.core.domain.model.LeagueSummary
import com.rmatch.football.core.domain.model.Lineup
import com.rmatch.football.core.domain.model.LineupMan
import com.rmatch.football.core.domain.model.MatchEvent
import com.rmatch.football.core.domain.model.MatchStatus
import com.rmatch.football.core.domain.model.OddsBoard
import com.rmatch.football.core.domain.model.OddsBookmaker
import com.rmatch.football.core.domain.model.OddsMarket
import com.rmatch.football.core.domain.model.OddsSelection
import com.rmatch.football.core.domain.model.PlayerProfile
import com.rmatch.football.core.domain.model.PlayerSeasonStats
import com.rmatch.football.core.domain.model.SquadMember
import com.rmatch.football.core.domain.model.StandingRow
import com.rmatch.football.core.domain.model.StatEntry
import com.rmatch.football.core.domain.model.TeamMatchStatistics
import com.rmatch.football.core.domain.model.TeamProfile
import com.rmatch.football.core.domain.model.TeamRef
import com.rmatch.football.core.domain.model.TeamSeasonStats
import com.rmatch.football.core.domain.model.Venue
import com.rmatch.football.core.network.dto.CoachDto
import com.rmatch.football.core.network.dto.CountryDto
import com.rmatch.football.core.network.dto.FixtureDto
import com.rmatch.football.core.network.dto.FixtureEventDto
import com.rmatch.football.core.network.dto.InjuryDto
import com.rmatch.football.core.network.dto.LeagueDto
import com.rmatch.football.core.network.dto.LeagueRefDto
import com.rmatch.football.core.network.dto.LineupDto
import com.rmatch.football.core.network.dto.LineupSlotDto
import com.rmatch.football.core.network.dto.OddsDto
import com.rmatch.football.core.network.dto.PlayerResponseDto
import com.rmatch.football.core.network.dto.SquadDto
import com.rmatch.football.core.network.dto.StandingRowDto
import com.rmatch.football.core.network.dto.StandingsResponseDto
import com.rmatch.football.core.network.dto.StatisticEntryDto
import com.rmatch.football.core.network.dto.TeamProfileDto
import com.rmatch.football.core.network.dto.TeamRefDto
import com.rmatch.football.core.network.dto.TeamStatisticsBlockDto
import com.rmatch.football.core.network.dto.TeamStatisticsDto
import com.rmatch.football.core.network.dto.VenueDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * DTO -> domain mappers. Every mapper drops records that miss mandatory
 * identifiers instead of inventing placeholder values.
 */

fun TeamRefDto.toDomainOrNull(): TeamRef? {
    val id = this.id ?: return null
    val name = this.name?.takeIf { it.isNotBlank() } ?: return null
    return TeamRef(id = id, name = name, logoUrl = logo)
}

fun VenueDto.toDomain(): Venue = Venue(
    id = id,
    name = name,
    city = city,
    capacity = capacity,
    surface = surface
)

fun LeagueRefDto.toDomainOrNull(): LeagueRef? {
    val id = this.id ?: return null
    val name = this.name?.takeIf { it.isNotBlank() } ?: return null
    return LeagueRef(
        id = id,
        name = name,
        country = country,
        logoUrl = logo,
        flagUrl = flag,
        season = season,
        round = round
    )
}

fun FixtureDto.toDomainOrNull(): Fixture? {
    val core = fixture ?: return null
    val id = core.id ?: return null
    val home = teams?.home?.toDomainOrNull() ?: return null
    val away = teams?.away?.toDomainOrNull() ?: return null
    val statusDto = core.status
    val status = if (statusDto?.short != null) {
        MatchStatus(
            shortCode = statusDto.short,
            description = statusDto.long ?: statusDto.short,
            elapsedMinutes = statusDto.elapsed
        )
    } else {
        MatchStatus.UNKNOWN
    }
    return Fixture(
        id = id,
        timestampSeconds = core.timestamp,
        dateIso = core.date,
        status = status,
        league = league?.toDomainOrNull(),
        home = home,
        away = away,
        homeGoals = goals?.home,
        awayGoals = goals?.away,
        halftimeHome = score?.halftime?.home,
        halftimeAway = score?.halftime?.away,
        venue = core.venue?.toDomain(),
        referee = core.referee?.takeIf { it.isNotBlank() }
    )
}

fun List<FixtureDto>.toFixtures(): List<Fixture> = mapNotNull { it.toDomainOrNull() }

fun CountryDto.toDomainOrNull(): Country? {
    val name = this.name?.takeIf { it.isNotBlank() } ?: return null
    return Country(name = name, code = code, flagUrl = flag)
}

fun LeagueDto.toDomainOrNull(): LeagueSummary? {
    val core = league ?: return null
    val id = core.id ?: return null
    val name = core.name?.takeIf { it.isNotBlank() } ?: return null
    val years = seasons?.mapNotNull { it.year }.orEmpty().sortedDescending()
    val current = seasons?.firstOrNull { it.current == true }?.year ?: years.firstOrNull()
    return LeagueSummary(
        league = LeagueRef(
            id = id,
            name = name,
            country = country?.name,
            logoUrl = core.logo,
            flagUrl = country?.flag,
            season = current
        ),
        countryName = country?.name,
        countryFlagUrl = country?.flag,
        seasons = years,
        currentSeason = current
    )
}

fun StandingRowDto.toDomainOrNull(): StandingRow? {
    val teamRef = team?.toDomainOrNull() ?: return null
    return StandingRow(
        rank = rank ?: 0,
        team = teamRef,
        points = points ?: 0,
        goalsDiff = goalsDiff ?: 0,
        group = group,
        form = form,
        played = all?.played ?: 0,
        win = all?.win ?: 0,
        draw = all?.draw ?: 0,
        lose = all?.lose ?: 0,
        goalsFor = all?.goals?.goalsFor ?: 0,
        goalsAgainst = all?.goals?.against ?: 0,
        description = description
    )
}

fun List<StandingsResponseDto>.toStandingRows(): List<StandingRow> =
    firstOrNull()?.league?.standings?.flatten()?.mapNotNull { it.toDomainOrNull() }.orEmpty()

private fun LineupSlotDto.toManOrNull(): LineupMan? {
    val p = player ?: return null
    val name = p.name?.takeIf { it.isNotBlank() } ?: return null
    return LineupMan(id = p.id, name = name, number = p.number, position = p.pos, grid = p.grid)
}

fun LineupDto.toDomain(): Lineup = Lineup(
    team = team?.toDomainOrNull(),
    coachName = coach?.name,
    formation = formation,
    startXI = startXI?.mapNotNull { it.toManOrNull() }.orEmpty(),
    substitutes = substitutes?.mapNotNull { it.toManOrNull() }.orEmpty()
)

fun JsonElement?.toStatValue(): String = when {
    this == null || this is JsonNull -> "—"
    this is JsonPrimitive -> content.takeIf { it.isNotBlank() && it != "null" } ?: "—"
    else -> toString()
}

fun StatisticEntryDto.toDomainOrNull(): StatEntry? {
    val label = type?.takeIf { it.isNotBlank() } ?: return null
    return StatEntry(type = label, value = value.toStatValue())
}

fun TeamStatisticsBlockDto.toDomain(): TeamMatchStatistics = TeamMatchStatistics(
    team = team?.toDomainOrNull(),
    entries = statistics?.mapNotNull { it.toDomainOrNull() }.orEmpty()
)

fun FixtureEventDto.toDomain(): MatchEvent = MatchEvent(
    minute = time?.elapsed,
    extraMinute = time?.extra,
    teamName = team?.name,
    playerName = player?.name,
    assistName = assist?.name,
    type = type,
    detail = detail
)

fun TeamProfileDto.toDomainOrNull(): TeamProfile? {
    val core = team ?: return null
    val id = core.id ?: return null
    val name = core.name?.takeIf { it.isNotBlank() } ?: return null
    return TeamProfile(
        team = TeamRef(id = id, name = name, logoUrl = core.logo),
        country = core.country,
        founded = core.founded,
        national = core.national == true,
        venue = venue?.toDomain()
    )
}

fun TeamStatisticsDto.toDomain(): TeamSeasonStats = TeamSeasonStats(
    form = form,
    played = fixtures?.played?.total,
    wins = fixtures?.wins?.total,
    draws = fixtures?.draws?.total,
    loses = fixtures?.loses?.total,
    goalsFor = goals?.goalsFor?.total?.total,
    goalsAgainst = goals?.against?.total?.total
)

fun SquadDto.toMembers(): List<SquadMember> = players.orEmpty().mapNotNull { p ->
    val name = p.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    SquadMember(
        id = p.id,
        name = name,
        age = p.age,
        number = p.number,
        position = p.position,
        photoUrl = p.photo
    )
}

fun CoachDto.toDomainOrNull(): Coach? {
    val name = this.name?.takeIf { it.isNotBlank() } ?: return null
    return Coach(id = id, name = name, age = age, nationality = nationality, photoUrl = photo)
}

fun InjuryDto.toDomainOrNull(): Injury? {
    val name = player?.name?.takeIf { it.isNotBlank() } ?: return null
    return Injury(
        playerId = player.id,
        playerName = name,
        teamId = team?.id,
        teamName = team?.name,
        type = player.type,
        reason = player.reason
    )
}

fun PlayerResponseDto.toDomainOrNull(): PlayerProfile? {
    val core = player ?: return null
    val id = core.id ?: return null
    val name = core.name?.takeIf { it.isNotBlank() } ?: return null
    return PlayerProfile(
        id = id,
        name = name,
        firstName = core.firstname,
        lastName = core.lastname,
        age = core.age,
        birthDate = core.birth?.date,
        birthPlace = core.birth?.place,
        nationality = core.nationality,
        height = core.height,
        weight = core.weight,
        injured = core.injured,
        photoUrl = core.photo,
        seasons = statistics.orEmpty().map { s ->
            PlayerSeasonStats(
                teamName = s.team?.name,
                leagueName = s.league?.name,
                appearances = s.games?.appearences,
                lineups = s.games?.lineups,
                minutes = s.games?.minutes,
                position = s.games?.position,
                rating = s.games?.rating,
                goals = s.goals?.total,
                assists = s.goals?.assists,
                yellowCards = s.cards?.yellow,
                redCards = s.cards?.red
            )
        }
    )
}

/** Odds strings are provider supplied; invalid values are discarded, never guessed. */
fun parseDecimalOdd(raw: String?): Double? {
    val normalized = raw?.trim()?.replace(',', '.') ?: return null
    val parsed = normalized.toDoubleOrNull() ?: return null
    if (parsed.isNaN() || parsed.isInfinite()) return null
    return if (parsed > 1.0) parsed else null
}

fun OddsDto.toDomain(): OddsBoard = OddsBoard(
    fixtureId = fixture?.id,
    updateIso = update,
    bookmakers = bookmakers.orEmpty().mapNotNull { bookmaker ->
        val bookmakerName = bookmaker.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        OddsBookmaker(
            id = bookmaker.id,
            name = bookmakerName,
            markets = bookmaker.bets.orEmpty().mapNotNull { bet ->
                val marketName = bet.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                OddsMarket(
                    id = bet.id,
                    name = marketName,
                    selections = bet.values.orEmpty().mapNotNull { v ->
                        val label = v.value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        OddsSelection(
                            label = label,
                            rawOdd = v.odd.orEmpty(),
                            decimalOdds = parseDecimalOdd(v.odd)
                        )
                    }
                )
            }
        )
    }
)
