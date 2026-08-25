package com.rmatch.football.core.util

/** Payload plus provenance metadata (used for the "stale data" badges). */
data class Loaded<out T>(
    val value: T,
    val fetchedAtMillis: Long,
    val fromCache: Boolean
)

sealed interface DataResult<out T> {
    data class Success<out T>(val loaded: Loaded<T>) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(
        Loaded(transform(loaded.value), loaded.fetchedAtMillis, loaded.fromCache)
    )
    is DataResult.Failure -> this
}
