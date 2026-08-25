package com.rmatch.football.feature.match_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.domain.analyst.AnalystResult
import com.rmatch.football.core.domain.analyst.OutcomeProbabilities
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.Injury
import com.rmatch.football.core.domain.model.Lineup
import com.rmatch.football.core.domain.model.MatchEvent
import com.rmatch.football.core.domain.model.TeamMatchStatistics
import com.rmatch.football.core.domain.usecase.AnalyzeFixtureUseCase
import com.rmatch.football.core.domain.usecase.FixtureAnalysis
import com.rmatch.football.core.domain.usecase.MarketComparison
import com.rmatch.football.core.domain.usecase.OddsComparator
import com.rmatch.football.core.util.AppError
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.core.util.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchDetailUiState(
    val fixtureId: Int,
    val selectedTab: Int = 0,
    val fixture: UiState<Fixture> = UiState.Loading,
    val events: UiState<List<MatchEvent>> = UiState.Loading,
    val analysis: UiState<FixtureAnalysis> = UiState.Loading,
    val lineups: UiState<List<Lineup>> = UiState.Loading,
    val statistics: UiState<List<TeamMatchStatistics>> = UiState.Loading,
    val odds: UiState<List<MarketComparison>> = UiState.Loading,
    val injuries: List<Injury> = emptyList()
)

class MatchDetailViewModel(
    private val repository: FootballRepository,
    private val analyzeFixture: AnalyzeFixtureUseCase,
    private val fixtureId: Int,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val internalState = MutableStateFlow(MatchDetailUiState(fixtureId = fixtureId))
    val state: StateFlow<MatchDetailUiState> = internalState.asStateFlow()

    private var analysisRequested = false
    private var lineupsRequested = false
    private var statisticsRequested = false
    private var oddsRequested = false

    init {
        loadOverview()
    }

    fun onTabSelected(index: Int) {
        internalState.value = internalState.value.copy(selectedTab = index)
        when (index) {
            1 -> if (!analysisRequested) loadAnalysis()
            2 -> if (!lineupsRequested) loadLineups()
            3 -> if (!statisticsRequested) loadStatistics()
            4 -> if (!oddsRequested) loadOdds()
        }
    }

    fun refreshCurrentTab() {
        when (internalState.value.selectedTab) {
            0 -> loadOverview(forceRefresh = true)
            1 -> loadAnalysis(forceRefresh = true)
            2 -> loadLineups(forceRefresh = true)
            3 -> loadStatistics(forceRefresh = true)
            4 -> loadOdds(forceRefresh = true)
        }
    }

    private fun loadOverview(forceRefresh: Boolean = false) {
        internalState.value = internalState.value.copy(fixture = UiState.Loading)
        viewModelScope.launch {
            val result = repository.fixture(fixtureId, forceRefresh)
            internalState.value = internalState.value.copy(
                fixture = when (result) {
                    is DataResult.Success -> result.loaded.value?.let {
                        UiState.Content(it, result.loaded.fetchedAtMillis, result.loaded.fromCache)
                    } ?: UiState.Empty(ErrorMessages.NO_VERIFIED_DATA)

                    is DataResult.Failure -> UiState.Error(ErrorMessages.of(result.error))
                }
            )
        }
        viewModelScope.launch {
            val events = repository.events(fixtureId, forceRefresh)
            internalState.value = internalState.value.copy(
                events = events.toUiState(isEmpty = { it.isEmpty() })
            )
        }
        viewModelScope.launch {
            val injuries = repository.injuries(fixtureId)
            if (injuries is DataResult.Success) {
                internalState.value = internalState.value.copy(injuries = injuries.loaded.value)
            }
        }
    }

    private fun loadAnalysis(forceRefresh: Boolean = false) {
        analysisRequested = true
        internalState.value = internalState.value.copy(analysis = UiState.Loading)
        viewModelScope.launch {
            val result = analyzeFixture(fixtureId, forceRefresh)
            internalState.value = internalState.value.copy(analysis = result.toUiState())
        }
    }

    private fun loadLineups(forceRefresh: Boolean = false) {
        lineupsRequested = true
        internalState.value = internalState.value.copy(lineups = UiState.Loading)
        viewModelScope.launch {
            val result = repository.lineups(fixtureId, forceRefresh)
            internalState.value = internalState.value.copy(
                lineups = result.toUiState(
                    emptyMessage = ErrorMessages.NO_LINEUPS,
                    isEmpty = { list -> list.isEmpty() || list.all { it.startXI.isEmpty() } }
                )
            )
        }
    }

    private fun loadStatistics(forceRefresh: Boolean = false) {
        statisticsRequested = true
        internalState.value = internalState.value.copy(statistics = UiState.Loading)
        viewModelScope.launch {
            val result = repository.statistics(fixtureId, forceRefresh)
            internalState.value = internalState.value.copy(
                statistics = result.toUiState(isEmpty = { it.isEmpty() })
            )
        }
    }

    private fun loadOdds(forceRefresh: Boolean = false) {
        oddsRequested = true
        internalState.value = internalState.value.copy(odds = UiState.Loading)
        viewModelScope.launch {
            if (!analysisRequested) loadAnalysis()
            val result = repository.odds(fixtureId, forceRefresh)
            val model = modelProbabilities()
            internalState.value = internalState.value.copy(
                odds = when (result) {
                    is DataResult.Success -> {
                        val comparisons = result.loaded.value.flatMap { board ->
                            OddsComparator.compareMatchWinner(board, model, nowProvider())
                        }
                        if (comparisons.isEmpty()) {
                            UiState.Empty(ErrorMessages.NO_ODDS)
                        } else {
                            UiState.Content(
                                comparisons,
                                result.loaded.fetchedAtMillis,
                                result.loaded.fromCache
                            )
                        }
                    }

                    is DataResult.Failure -> if (result.error is AppError.EmptyResponse) {
                        UiState.Empty(ErrorMessages.NO_ODDS)
                    } else {
                        UiState.Error(ErrorMessages.of(result.error))
                    }
                }
            )
        }
    }

    private fun modelProbabilities(): OutcomeProbabilities? {
        val analysis = internalState.value.analysis
        if (analysis !is UiState.Content) return null
        val result = analysis.data.result
        return (result as? AnalystResult.Ready)?.report?.outcome
    }
}
