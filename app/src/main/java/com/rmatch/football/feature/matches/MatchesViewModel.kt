package com.rmatch.football.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.NetworkMonitor
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MatchesFilter(val title: String) {
    YESTERDAY("Вчера"),
    TODAY("Сегодня"),
    LIVE("Live"),
    TOMORROW("Завтра")
}

data class MatchesUiState(
    val filter: MatchesFilter = MatchesFilter.TODAY,
    val query: String = "",
    val countryFilter: String = "",
    val leagueFilter: String = "",
    val statusFilter: String = "",
    val online: Boolean = true,
    val content: UiState<List<Fixture>> = UiState.Loading
)

class MatchesViewModel(
    private val repository: FootballRepository,
    private val networkMonitor: NetworkMonitor,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val internalState = MutableStateFlow(MatchesUiState())
    val state: StateFlow<MatchesUiState> = internalState.asStateFlow()

    init {
        observeNetwork()
        refresh()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.online.collect { online ->
                internalState.value = internalState.value.copy(online = online)
            }
        }
    }

    fun onFilterSelected(filter: MatchesFilter) {
        if (filter == internalState.value.filter) return
        internalState.value = internalState.value.copy(filter = filter)
        refresh()
    }

    fun onQueryChanged(value: String) {
        internalState.value = internalState.value.copy(query = value)
    }

    fun onCountryFilterChanged(value: String) {
        internalState.value = internalState.value.copy(countryFilter = value)
    }

    fun onLeagueFilterChanged(value: String) {
        internalState.value = internalState.value.copy(leagueFilter = value)
    }

    fun onStatusFilterChanged(value: String) {
        internalState.value = internalState.value.copy(statusFilter = value)
    }

    fun refresh(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(content = UiState.Loading)
        viewModelScope.launch {
            val today = todayProvider()
            val result = when (internalState.value.filter) {
                MatchesFilter.YESTERDAY -> repository.fixturesByDate(today.minusDays(1), forceRefresh)
                MatchesFilter.TODAY -> repository.fixturesByDate(today, forceRefresh)
                MatchesFilter.TOMORROW -> repository.fixturesByDate(today.plusDays(1), forceRefresh)
                MatchesFilter.LIVE -> repository.liveFixtures(forceRefresh)
            }
            internalState.value = internalState.value.copy(
                content = result.toUiState(
                    emptyMessage = ErrorMessages.NO_VERIFIED_DATA,
                    isEmpty = { it.isEmpty() }
                )
            )
        }
    }
}

/** Pure filtering + grouping helpers (unit tested). */
object MatchesGrouping {

    fun filter(
        fixtures: List<Fixture>,
        query: String,
        countryFilter: String = "",
        leagueFilter: String = "",
        statusFilter: String = ""
    ): List<Fixture> {
        var result = fixtures

        val trimmed = query.trim().lowercase()
        if (trimmed.isNotEmpty()) {
            result = result.filter { fixture ->
                fixture.home.name.lowercase().contains(trimmed) ||
                    fixture.away.name.lowercase().contains(trimmed) ||
                    fixture.league?.name?.lowercase()?.contains(trimmed) == true ||
                    fixture.league?.country?.lowercase()?.contains(trimmed) == true
            }
        }

        val country = countryFilter.trim().lowercase()
        if (country.isNotEmpty()) {
            result = result.filter {
                it.league?.country?.lowercase()?.contains(country) == true
            }
        }

        val league = leagueFilter.trim().lowercase()
        if (league.isNotEmpty()) {
            result = result.filter {
                it.league?.name?.lowercase()?.contains(league) == true
            }
        }

        when (statusFilter) {
            "live" -> result = result.filter { it.status.isLive }
            "upcoming" -> result = result.filter { it.status.isUpcoming }
            "finished" -> result = result.filter { it.status.isFinished }
        }

        return result
    }

    fun groupByDate(fixtures: List<Fixture>): List<Pair<String, List<Fixture>>> =
        fixtures
            .sortedBy { it.timestampSeconds ?: Long.MAX_VALUE }
            .groupBy { TimeFormat.date(it.timestampSeconds) }
            .toList()
}
