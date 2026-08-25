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

private data class AuthState(val apiKey: String?, val busy: Boolean, val error: String?)
private data class CatalogState(
    val apiHealth: ScreenData<ApiHealth>,
    val countries: ScreenData<List<Country>>,
    val seasons: ScreenData<List<Int>>,
    val leagues: ScreenData<List<LeagueSummary>>,
    val matches: ScreenData<List<MatchSummary>>,
)
private data class DetailState(
    val analytics: ScreenData<List<Pair<MatchSummary, MatchAnalysis?>>>,
    val selectedLeague: ScreenData<LeagueDetails>,
    val selectedMatch: ScreenData<MatchDetails>,
    val selectedTeam: ScreenData<TeamProfile>,
    val quota: QuotaInfo,
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

    private val authState = combine(repository.apiKeyFlow, onboardingBusy, onboardingError) { apiKey, busy, error ->
        AuthState(apiKey, busy, error)
    }
    private val catalogState = combine(apiHealth, countries, seasons, leagues, matches) { health, countriesValue, seasonsValue, leaguesValue, matchesValue ->
        CatalogState(health, countriesValue, seasonsValue, leaguesValue, matchesValue)
    }
    private val detailState = combine(analytics, selectedLeague, selectedMatch, selectedTeam, repository.quotaFlow) { analyticsValue, leagueValue, matchValue, teamValue, quota ->
        DetailState(analyticsValue, leagueValue, matchValue, teamValue, quota)
    }

    val uiState: StateFlow<MainUiState> = combine(
        authState,
        catalogState,
        detailState,
        countryFilter,
        leagueFilter,
    ) { auth, catalog, detail, country, league ->
        MainUiState(
            apiKeyPresent = !auth.apiKey.isNullOrBlank(),
            maskedApiKey = repository.maskApiKey(),
            onboardingBusy = auth.busy,
            onboardingError = auth.error,
            apiHealth = catalog.apiHealth,
            countries = catalog.countries,
            seasons = catalog.seasons,
            leagues = catalog.leagues,
            matches = catalog.matches,
            analytics = detail.analytics,
            selectedLeague = detail.selectedLeague,
            selectedMatch = detail.selectedMatch,
            selectedTeam = detail.selectedTeam,
            quota = detail.quota,
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
