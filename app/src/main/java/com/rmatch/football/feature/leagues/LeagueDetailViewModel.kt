package com.rmatch.football.feature.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.StandingRow
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeagueDetailUiState(
    val leagueId: Int,
    val season: Int,
    val selectedTab: Int = 0,
    val standings: UiState<List<StandingRow>> = UiState.Loading,
    val fixtures: UiState<List<Fixture>> = UiState.Loading
)

class LeagueDetailViewModel(
    private val repository: FootballRepository,
    leagueId: Int,
    season: Int
) : ViewModel() {

    private val internalState = MutableStateFlow(
        LeagueDetailUiState(leagueId = leagueId, season = season)
    )
    val state: StateFlow<LeagueDetailUiState> = internalState.asStateFlow()

    init {
        refresh()
    }

    fun onTabSelected(index: Int) {
        internalState.value = internalState.value.copy(selectedTab = index)
    }

    fun refresh(forceRefresh: Boolean = false) {
        val current = internalState.value
        internalState.value = current.copy(
            standings = UiState.Loading,
            fixtures = UiState.Loading
        )
        viewModelScope.launch {
            val standings = repository.standings(current.leagueId, current.season, forceRefresh)
            internalState.value = internalState.value.copy(
                standings = standings.toUiState(
                    emptyMessage = ErrorMessages.NO_VERIFIED_DATA,
                    isEmpty = { it.isEmpty() }
                )
            )
        }
        viewModelScope.launch {
            val fixtures = repository.nextFixturesForLeague(
                leagueId = current.leagueId,
                season = current.season,
                next = 10,
                forceRefresh = forceRefresh
            )
            internalState.value = internalState.value.copy(
                fixtures = fixtures.toUiState(
                    emptyMessage = ErrorMessages.NO_VERIFIED_DATA,
                    isEmpty = { it.isEmpty() }
                )
            )
        }
    }
}
