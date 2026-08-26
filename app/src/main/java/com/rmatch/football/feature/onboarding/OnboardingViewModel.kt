package com.rmatch.football.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.datastore.SettingsDataStore
import com.rmatch.football.core.domain.model.ProviderStatus
import com.rmatch.football.core.security.ApiKeyValidator
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedMode: StartupMode? = null,
    val key: String = "",
    val checking: Boolean = false,
    val error: String? = null,
    val status: ProviderStatus? = null,
    val completed: Boolean = false
)

enum class StartupMode {
    FREE_APIS,
    PAID_KEY
}

class OnboardingViewModel(
    private val repository: FootballRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val internalState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = internalState.asStateFlow()

    fun onKeyChanged(value: String) {
        internalState.value = internalState.value.copy(key = value, error = null)
    }

    fun chooseFreeApis() {
        internalState.value = internalState.value.copy(checking = true, error = null, selectedMode = StartupMode.FREE_APIS)
        viewModelScope.launch {
            settings.setOnboardingCompleted(true)
            internalState.value = internalState.value.copy(checking = false, completed = true)
        }
    }

    fun choosePaidKey() {
        internalState.value = internalState.value.copy(
            selectedMode = StartupMode.PAID_KEY,
            error = null
        )
    }

    fun backToChoice() {
        internalState.value = internalState.value.copy(
            selectedMode = null,
            key = "",
            error = null,
            checking = false
        )
    }

    fun submit() {
        val candidate = internalState.value.key.trim()
        if (!ApiKeyValidator.isPlausible(candidate)) {
            internalState.value = internalState.value.copy(
                error = "Ключ выглядит некорректно: ожидается ${ApiKeyValidator.MIN_LENGTH}–" +
                    "${ApiKeyValidator.MAX_LENGTH} латинских символов или цифр."
            )
            return
        }
        internalState.value = internalState.value.copy(checking = true, error = null)
        viewModelScope.launch {
            when (val result = repository.validateAndSaveKey(candidate)) {
                is DataResult.Success -> {
                    settings.setOnboardingCompleted(true)
                    internalState.value = internalState.value.copy(
                        checking = false,
                        status = result.loaded.value,
                        completed = true,
                        key = ""
                    )
                }

                is DataResult.Failure -> internalState.value = internalState.value.copy(
                    checking = false,
                    error = ErrorMessages.of(result.error)
                )
            }
        }
    }
}
