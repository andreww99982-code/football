package com.rmatch.football.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.Coach
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.SquadMember
import com.rmatch.football.core.domain.model.TeamProfile
import com.rmatch.football.core.domain.usecase.toSamples
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamUiState(
    val teamId: Int,
    val profile: UiState<TeamProfile> = UiState.Loading,
    val lastMatches: UiState<List<Fixture>> = UiState.Loading,
    val nextMatches: UiState<List<Fixture>> = UiState.Loading,
    val squad: UiState<List<SquadMember>> = UiState.Loading,
    val coaches: List<Coach> = emptyList(),
    val form: String = ""
)

class TeamViewModel(
    private val repository: FootballRepository,
    private val teamId: Int
) : ViewModel() {

    private val internalState = MutableStateFlow(TeamUiState(teamId = teamId))
    val state: StateFlow<TeamUiState> = internalState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(
            profile = UiState.Loading,
            lastMatches = UiState.Loading,
            nextMatches = UiState.Loading,
            squad = UiState.Loading
        )
        viewModelScope.launch {
            val result = repository.teamProfile(teamId, forceRefresh)
            internalState.value = internalState.value.copy(
                profile = when (result) {
                    is DataResult.Success -> result.loaded.value?.let {
                        UiState.Content(it, result.loaded.fetchedAtMillis, result.loaded.fromCache)
                    } ?: UiState.Empty(ErrorMessages.NO_VERIFIED_DATA)

                    is DataResult.Failure -> UiState.Error(ErrorMessages.of(result.error))
                }
            )
        }
        viewModelScope.launch {
            val result = repository.lastFixturesForTeam(teamId, 10, forceRefresh)
            internalState.value = internalState.value.copy(
                lastMatches = result.toUiState(isEmpty = { it.isEmpty() }),
                form = when (result) {
                    is DataResult.Success -> result.loaded.value.toSamples(teamId)
                        .take(5)
                        .joinToString(" ") { it.resultLetter }

                    is DataResult.Failure -> ""
                }
            )
        }
        viewModelScope.launch {
            val result = repository.nextFixturesForTeam(teamId, 5, forceRefresh)
            internalState.value = internalState.value.copy(
                nextMatches = result.toUiState(isEmpty = { it.isEmpty() })
            )
        }
        viewModelScope.launch {
            val result = repository.squad(teamId, forceRefresh)
            internalState.value = internalState.value.copy(
                squad = result.toUiState(isEmpty = { it.isEmpty() })
            )
        }
        viewModelScope.launch {
            val result = repository.coaches(teamId, forceRefresh)
            if (result is DataResult.Success) {
                internalState.value = internalState.value.copy(coaches = result.loaded.value)
            }
        }
    }
}
