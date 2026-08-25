package com.rmatch.football.core.util

/** Generic screen state used by every feature ViewModel. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(
        val data: T,
        val fetchedAtMillis: Long,
        val fromCache: Boolean
    ) : UiState<T>

    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>
}

fun <T> DataResult<T>.toUiState(
    emptyMessage: String = ErrorMessages.NO_VERIFIED_DATA,
    isEmpty: (T) -> Boolean = { false }
): UiState<T> = when (this) {
    is DataResult.Success -> if (isEmpty(loaded.value)) {
        UiState.Empty(emptyMessage)
    } else {
        UiState.Content(loaded.value, loaded.fetchedAtMillis, loaded.fromCache)
    }
    is DataResult.Failure -> UiState.Error(ErrorMessages.of(error))
}
