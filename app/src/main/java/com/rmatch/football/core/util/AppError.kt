package com.rmatch.football.core.util

/** Domain level error taxonomy. UI messages are Russian and neutral. */
sealed interface AppError {
    data object NoApiKey : AppError
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object Offline : AppError
    data object EmptyResponse : AppError
    data class Http(val code: Int) : AppError
    data class Network(val reason: String) : AppError
    data class Api(val reason: String) : AppError
}

object ErrorMessages {

    const val NO_VERIFIED_DATA = "Нет верифицированных данных"
    const val NO_LINEUPS = "Составы пока не опубликованы поставщиком"
    const val NOT_ENOUGH_DATA = "Недостаточно данных для расчёта"
    const val NO_ODDS = "Коэффициенты недоступны"
    const val QUOTA_EXCEEDED = "Лимит API исчерпан"

    fun of(error: AppError): String = when (error) {
        AppError.NoApiKey -> "API-ключ не задан. Добавьте ключ в настройках."
        AppError.Unauthorized -> "Ключ недействителен (401). Проверьте ключ в настройках."
        AppError.RateLimited -> QUOTA_EXCEEDED
        AppError.Offline -> "Нет подключения к сети. Показаны кэшированные данные, если они есть."
        AppError.EmptyResponse -> NO_VERIFIED_DATA
        is AppError.Http -> "Ошибка сервера (${error.code}). Повторите попытку позже."
        is AppError.Network -> "Сетевая ошибка: ${error.reason}"
        is AppError.Api -> "Поставщик данных вернул ошибку: ${error.reason}"
    }
}
