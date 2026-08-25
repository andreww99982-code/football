package com.rmatch.football.feature.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.LeagueSummary
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaguesUiState(
    val season: Int,
    val query: String = "",
    val content: UiState<List<LeagueSummary>> = UiState.Loading
)

class LeaguesViewModel(
    private val repository: FootballRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val internalState = MutableStateFlow(
        LeaguesUiState(season = TimeFormat.currentSeason(todayProvider()))
    )
    val state: StateFlow<LeaguesUiState> = internalState.asStateFlow()

    init {
        refresh()
    }

    fun onQueryChanged(value: String) {
        internalState.value = internalState.value.copy(query = value)
    }

    fun refresh(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(content = UiState.Loading)
        viewModelScope.launch {
            val result = repository.leagues(internalState.value.season, forceRefresh)
            internalState.value = internalState.value.copy(
                content = result.toUiState(
                    emptyMessage = ErrorMessages.NO_VERIFIED_DATA,
                    isEmpty = { it.isEmpty() }
                )
            )
        }
    }
}

object LeagueFiltering {
    fun filter(leagues: List<LeagueSummary>, query: String): List<LeagueSummary> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return leagues
        return leagues.filter {
            it.league.name.lowercase().contains(trimmed) ||
                it.countryName?.lowercase()?.contains(trimmed) == true
        }
    }

    fun groupByCountry(leagues: List<LeagueSummary>): List<Pair<String, List<LeagueSummary>>> =
        leagues
            .sortedWith(compareBy({ it.countryName ?: "" }, { it.league.name }))
            .groupBy { it.countryName ?: "Без страны" }
            .toList()
}
