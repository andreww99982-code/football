package com.rmatch.football.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatting helpers. All timestamps come from the provider, nothing is invented. */
object TimeFormat {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru"))

    fun time(epochSeconds: Long?, zone: ZoneId = ZoneId.systemDefault()): String =
        epochSeconds?.let {
            timeFormatter.format(Instant.ofEpochSecond(it).atZone(zone))
        } ?: "—"

    fun date(epochSeconds: Long?, zone: ZoneId = ZoneId.systemDefault()): String =
        epochSeconds?.let {
            dateFormatter.format(Instant.ofEpochSecond(it).atZone(zone))
        } ?: "—"

    fun dateTimeMillis(epochMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): String =
        epochMillis?.let {
            dateTimeFormatter.format(Instant.ofEpochMilli(it).atZone(zone))
        } ?: "—"

    fun localDate(epochSeconds: Long?, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        epochSeconds?.let { Instant.ofEpochSecond(it).atZone(zone).toLocalDate() }

    fun isoDate(date: LocalDate): String = date.toString()

    fun relativeAge(fetchedAtMillis: Long, nowMillis: Long): String {
        val deltaSec = ((nowMillis - fetchedAtMillis) / 1000L).coerceAtLeast(0L)
        return when {
            deltaSec < 60 -> "$deltaSec с назад"
            deltaSec < 3600 -> "${deltaSec / 60} мин назад"
            deltaSec < 86400 -> "${deltaSec / 3600} ч назад"
            else -> "${deltaSec / 86400} дн назад"
        }
    }

    /** Current football season used by API-Football (season starts in July). */
    fun currentSeason(today: LocalDate): Int =
        if (today.monthValue >= 7) today.year else today.year - 1
}
