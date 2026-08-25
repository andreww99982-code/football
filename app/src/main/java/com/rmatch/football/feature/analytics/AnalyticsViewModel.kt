package com.rmatch.football.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val dayOffset: Int = 0,
    val content: UiState<List<Fixture>> = UiState.Loading
)

class AnalyticsViewModel(
    private val repository: FootballRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val internalState = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = internalState.asStateFlow()

    init {
        refresh()
    }

    fun onDayOffsetSelected(offset: Int) {
        if (offset == internalState.value.dayOffset) return
        internalState.value = internalState.value.copy(dayOffset = offset)
        refresh()
    }

    fun refresh(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(content = UiState.Loading)
        viewModelScope.launch {
            val date = todayProvider().plusDays(internalState.value.dayOffset.toLong())
            val result = repository.fixturesByDate(date, forceRefresh)
            internalState.value = internalState.value.copy(
                content = result
                    .toUiState(
                        emptyMessage = ErrorMessages.NO_VERIFIED_DATA,
                        isEmpty = { list -> list.none { !it.status.isFinished } }
                    )
            )
        }
    }
}
