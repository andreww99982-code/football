package com.rmatch.football.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.PlayerProfile
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.core.util.UiState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val playerId: Int,
    val season: Int,
    val content: UiState<PlayerProfile> = UiState.Loading
)

class PlayerViewModel(
    private val repository: FootballRepository,
    private val playerId: Int,
    todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val internalState = MutableStateFlow(
        PlayerUiState(playerId = playerId, season = TimeFormat.currentSeason(todayProvider()))
    )
    val state: StateFlow<PlayerUiState> = internalState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(content = UiState.Loading)
        viewModelScope.launch {
            val result = repository.players(
                playerId = playerId,
                season = internalState.value.season,
                forceRefresh = forceRefresh
            )
            internalState.value = internalState.value.copy(
                content = when (result) {
                    is DataResult.Success -> result.loaded.value.firstOrNull()?.let {
                        UiState.Content(it, result.loaded.fetchedAtMillis, result.loaded.fromCache)
                    } ?: UiState.Empty(ErrorMessages.NO_VERIFIED_DATA)

                    is DataResult.Failure -> UiState.Error(ErrorMessages.of(result.error))
                }
            )
        }
    }
}
