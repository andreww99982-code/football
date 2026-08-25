package com.rogermichin.rmatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rogermichin.rmatch.data.ApiHealth
import com.rogermichin.rmatch.data.Country
import com.rogermichin.rmatch.data.DataResource
import com.rogermichin.rmatch.data.FootballRepository
import com.rogermichin.rmatch.data.LeagueDetails
import com.rogermichin.rmatch.data.LeagueSummary
import com.rogermichin.rmatch.data.MatchAnalysis
import com.rogermichin.rmatch.data.MatchDetails
import com.rogermichin.rmatch.data.MatchSummary
import com.rogermichin.rmatch.data.QuotaInfo
import com.rogermichin.rmatch.data.ScreenData
import com.rogermichin.rmatch.data.TeamProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val apiKeyPresent: Boolean = false,
    val maskedApiKey: String = "Ключ не задан",
    val onboardingBusy: Boolean = false,
    val onboardingError: String? = null,
    val apiHealth: ScreenData<ApiHealth> = ScreenData(),
    val countries: ScreenData<List<Country>> = ScreenData(),
    val seasons: ScreenData<List<Int>> = ScreenData(),
    val leagues: ScreenData<List<LeagueSummary>> = ScreenData(),
    val matches: ScreenData<List<MatchSummary>> = ScreenData(),
    val analytics: ScreenData<List<Pair<MatchSummary, MatchAnalysis?>>> = ScreenData(),
    val selectedLeague: ScreenData<LeagueDetails> = ScreenData(),
    val selectedMatch: ScreenData<MatchDetails> = ScreenData(),
    val selectedTeam: ScreenData<TeamProfile> = ScreenData(),
    val quota: QuotaInfo = QuotaInfo(),
    val countryFilter: String = "",
    val leagueFilter: String = "",
)

class MainViewModel(private val repository: FootballRepository) : ViewModel() {
    private val apiHealth = MutableStateFlow(ScreenData<ApiHealth>())
    private val countries = MutableStateFlow(ScreenData<List<Country>>())
    private val seasons = MutableStateFlow(ScreenData<List<Int>>())
    private val leagues = MutableStateFlow(ScreenData<List<LeagueSummary>>())
    private val matches = MutableStateFlow(ScreenData<List<MatchSummary>>())
    private val analytics = MutableStateFlow(ScreenData<List<Pair<MatchSummary, MatchAnalysis?>>>())
    private val selectedLeague = MutableStateFlow(ScreenData<LeagueDetails>())
    private val selectedMatch = MutableStateFlow(ScreenData<MatchDetails>())
    private val selectedTeam = MutableStateFlow(ScreenData<TeamProfile>())
    private val onboardingBusy = MutableStateFlow(false)
    private val onboardingError = MutableStateFlow<String?>(null)
    private val countryFilter = repository.countryFilter.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val leagueFilter = repository.leagueFilter.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val uiState: StateFlow<MainUiState> = combine(
        repository.apiKeyFlow,
        onboardingBusy,
        onboardingError,
        apiHealth,
        countries,
        seasons,
        leagues,
        matches,
        analytics,
        selectedLeague,
        selectedMatch,
        selectedTeam,
        repository.quotaFlow,
        countryFilter,
        leagueFilter,
    ) { apiKey, busy, error, health, countriesValue, seasonsValue, leaguesValue, matchesValue, analyticsValue, leagueValue, matchValue, teamValue, quota, country, league ->
        MainUiState(
            apiKeyPresent = !apiKey.isNullOrBlank(),
            maskedApiKey = repository.maskApiKey(),
            onboardingBusy = busy,
            onboardingError = error,
            apiHealth = health,
            countries = countriesValue,
            seasons = seasonsValue,
            leagues = leaguesValue,
            matches = matchesValue,
            analytics = analyticsValue,
            selectedLeague = leagueValue,
            selectedMatch = matchValue,
            selectedTeam = teamValue,
            quota = quota,
            countryFilter = country,
            leagueFilter = league,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            repository.apiKeyFlow.collect { key ->
                if (key.isNullOrBlank()) {
                    apiHealth.value = ScreenData()
                    countries.value = ScreenData()
                    seasons.value = ScreenData()
                    leagues.value = ScreenData()
                    matches.value = ScreenData()
                    analytics.value = ScreenData()
                } else {
                    refreshDashboard(false)
                }
            }
        }
    }

    fun verifyAndSaveApiKey(candidate: String) {
        viewModelScope.launch {
            onboardingBusy.value = true
            onboardingError.value = null
            repository.verifyAndSaveApiKey(candidate)
                .onSuccess {
                    apiHealth.value = ScreenData(value = it)
                    refreshDashboard(true)
                }
                .onFailure { onboardingError.value = it.message }
            onboardingBusy.value = false
        }
    }

    fun refreshDashboard(force: Boolean = true) {
        viewModelScope.launch {
            loadInto(apiHealth) { repository.getApiHealth(force) }
            loadInto(countries) { repository.getCountries(force) }
            loadInto(seasons) { repository.getSeasons(force) }
            loadInto(leagues) { repository.getLeagues(countryFilter.value, forceRefresh = force) }
            loadInto(matches) { repository.getMatches(forceRefresh = force) }
            loadInto(analytics) { repository.getAnalyticsCards(force) }
        }
    }

    fun selectLeague(leagueId: Int, season: Int) { viewModelScope.launch { loadInto(selectedLeague) { repository.getLeagueDetails(leagueId, season, true) } } }
    fun selectMatch(fixtureId: Int, leagueId: Int, season: Int) { viewModelScope.launch { loadInto(selectedMatch) { repository.getMatchDetails(fixtureId, leagueId, season, true) } } }
    fun selectTeam(teamId: Int, leagueId: Int, season: Int) { viewModelScope.launch { loadInto(selectedTeam) { repository.getTeamProfile(teamId, leagueId, season, true) } } }
    fun saveCountryFilter(value: String) { viewModelScope.launch { repository.saveCountryFilter(value); loadInto(leagues) { repository.getLeagues(value, forceRefresh = true) } } }
    fun saveLeagueFilter(value: String) { viewModelScope.launch { repository.saveLeagueFilter(value) } }
    fun clearCache() { viewModelScope.launch { repository.clearCache(); refreshDashboard(true) } }
    fun deleteApiKeyAndCache() { viewModelScope.launch { repository.deleteApiKeyAndCache(); onboardingError.value = null } }

    private suspend fun <T> loadInto(state: MutableStateFlow<ScreenData<T>>, block: suspend () -> DataResource<T>) {
        state.value = state.value.copy(loading = true, error = null)
        runCatching { block() }
            .onSuccess { resource ->
                val emptyMessage = (resource.data as? List<*>)?.takeIf { it.isEmpty() }?.let { "Нет верифицированных данных" }
                state.value = ScreenData(value = resource.data, meta = resource.meta, loading = false, emptyMessage = emptyMessage)
            }
            .onFailure { error ->
                state.value = ScreenData(value = state.value.value, meta = state.value.meta, loading = false, error = error.message, emptyMessage = if (state.value.value == null) "Нет верифицированных данных" else null)
            }
    }
}

class MainViewModelFactory(private val repository: FootballRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
}
