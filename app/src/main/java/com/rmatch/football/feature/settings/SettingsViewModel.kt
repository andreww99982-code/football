package com.rmatch.football.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rmatch.football.core.data.FootballRepository
import com.rmatch.football.core.datastore.SettingsDataStore
import com.rmatch.football.core.domain.model.ProviderStatus
import com.rmatch.football.core.domain.model.QuotaInfo
import com.rmatch.football.core.security.ApiKeyStorage
import com.rmatch.football.core.security.ApiKeyValidator
import com.rmatch.football.core.util.DataResult
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val sourceName: String = "Бесплатные API",
    val maskedKey: String = "—",
    val hasKey: Boolean = false,
    val online: Boolean = true,
    val checking: Boolean = false,
    val status: ProviderStatus? = null,
    val statusMessage: String? = null,
    val quota: QuotaInfo = QuotaInfo(null, null, null),
    val cachedEntries: Int = 0,
    val lastCacheUpdate: Long? = null,
    val newKeyInput: String = "",
    val newKeyError: String? = null,
    val savingKey: Boolean = false,
    val message: String? = null,
    val keyRemoved: Boolean = false
)

class SettingsViewModel(
    private val repository: FootballRepository,
    private val keyStorage: ApiKeyStorage,
    private val networkMonitor: NetworkMonitor,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val internalState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = internalState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.quota.collect { quota ->
                internalState.value = internalState.value.copy(quota = quota)
            }
        }
    }

    fun refresh() {
        internalState.value = internalState.value.copy(
            sourceName = if (keyStorage.hasKey()) "API-Football (личный ключ)" else "TheSportsDB + OpenLigaDB",
            maskedKey = ApiKeyValidator.mask(keyStorage.getKey()),
            hasKey = keyStorage.hasKey(),
            online = networkMonitor.isOnline()
        )
        viewModelScope.launch {
            internalState.value = internalState.value.copy(
                cachedEntries = repository.cachedEntries(),
                lastCacheUpdate = repository.lastCacheUpdate()
            )
        }
    }

    fun checkConnection() {
        if (!keyStorage.hasKey()) {
            internalState.value = internalState.value.copy(
                statusMessage = "Личный ключ не сохранён. Работают бесплатные источники, если у них есть покрытие."
            )
            return
        }
        internalState.value = internalState.value.copy(checking = true, statusMessage = null)
        viewModelScope.launch {
            val online = networkMonitor.isOnline()
            when (val result = repository.providerStatus(forceRefresh = true)) {
                is DataResult.Success -> internalState.value = internalState.value.copy(
                    checking = false,
                    online = online,
                    status = result.loaded.value,
                    statusMessage = "Соединение с провайдером подтверждено."
                )

                is DataResult.Failure -> internalState.value = internalState.value.copy(
                    checking = false,
                    online = online,
                    statusMessage = ErrorMessages.of(result.error)
                )
            }
            internalState.value = internalState.value.copy(
                cachedEntries = repository.cachedEntries(),
                lastCacheUpdate = repository.lastCacheUpdate()
            )
        }
    }

    fun onNewKeyChanged(value: String) {
        internalState.value = internalState.value.copy(newKeyInput = value, newKeyError = null)
    }

    fun saveNewKey() {
        val candidate = internalState.value.newKeyInput.trim()
        if (!ApiKeyValidator.isPlausible(candidate)) {
            internalState.value = internalState.value.copy(
                newKeyError = "Ключ должен содержать от ${ApiKeyValidator.MIN_LENGTH} до " +
                    "${ApiKeyValidator.MAX_LENGTH} латинских букв и цифр."
            )
            return
        }
        internalState.value = internalState.value.copy(savingKey = true, newKeyError = null)
        viewModelScope.launch {
            when (val result = repository.validateAndSaveKey(candidate)) {
                is DataResult.Success -> {
                    repository.clearCache()
                    internalState.value = internalState.value.copy(
                        savingKey = false,
                        newKeyInput = "",
                        status = result.loaded.value,
                        maskedKey = ApiKeyValidator.mask(keyStorage.getKey()),
                        hasKey = keyStorage.hasKey(),
                        message = "Ключ обновлён и проверен у провайдера.",
                        cachedEntries = 0,
                        lastCacheUpdate = null
                    )
                }

                is DataResult.Failure -> internalState.value = internalState.value.copy(
                    savingKey = false,
                    newKeyError = ErrorMessages.of(result.error)
                )
            }
        }
    }

    fun deleteKey() {
        viewModelScope.launch {
            repository.clearKeyAndCache()
            settings.setOnboardingCompleted(false)
            internalState.value = SettingsUiState(
                sourceName = "Бесплатные API",
                maskedKey = "—",
                hasKey = false,
                online = networkMonitor.isOnline(),
                message = "Ключ удалён, кэш очищен.",
                keyRemoved = true
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            internalState.value = internalState.value.copy(
                cachedEntries = 0,
                lastCacheUpdate = null,
                message = "Кэш очищен."
            )
        }
    }

    fun consumeMessage() {
        internalState.value = internalState.value.copy(message = null)
    }
}
