package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.MatchDetails
import com.rogermichin.rmatch.data.ScreenData

@Composable
fun MatchDetailsScreen(state: ScreenData<MatchDetails>, onBack: () -> Unit, onTeamClick: (Int, Int, Int) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Обзор", "Аналитика", "Составы", "Статистика", "Коэффициенты")
    ScreenContainer(state.value?.summary?.let { "${it.homeTeam.name} — ${it.awayTeam.name}" } ?: "Матч", state.value?.summary?.leagueName, state, onBack) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title -> Tab(selected = index == selectedTab, onClick = { selectedTab = index }, text = { Text(title) }) }
        }
        state.value?.let { details ->
            when (selectedTab) {
                0 -> OverviewTab(details, onTeamClick)
                1 -> AnalyticsTab(details)
                2 -> LineupsTab(details)
                3 -> StatisticsTab(details)
                4 -> OddsTab(details)
            }
        }
    }
}

@Composable
private fun OverviewTab(details: MatchDetails, onTeamClick: (Int, Int, Int) -> Unit) {
    SectionCard("Обзор") {
        Text("Счёт: ${details.summary.homeGoals ?: "—"}:${details.summary.awayGoals ?: "—"}")
        Text("Статус: ${details.summary.status}")
        Text("Venue: ${details.summary.venue ?: "Нет верифицированных данных"}")
        Text("Referee: ${details.summary.referee ?: "Нет верифицированных данных"}")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(details.summary.homeTeam.name, modifier = Modifier.clickable { onTeamClick(details.summary.homeTeam.id, details.summary.leagueId, details.summary.season) }, fontWeight = FontWeight.Bold)
            Text(details.summary.awayTeam.name, modifier = Modifier.clickable { onTeamClick(details.summary.awayTeam.id, details.summary.leagueId, details.summary.season) }, fontWeight = FontWeight.Bold)
        }
        Text("Форма хозяев")
        details.homeForm.take(5).forEach { Text("• ${it.homeTeam.name} ${it.homeGoals ?: "—"}:${it.awayGoals ?: "—"} ${it.awayTeam.name}") }
        Text("Форма гостей")
        details.awayForm.take(5).forEach { Text("• ${it.homeTeam.name} ${it.homeGoals ?: "—"}:${it.awayGoals ?: "—"} ${it.awayTeam.name}") }
        Text("События")
        if (details.events.isEmpty()) Text("Нет верифицированных данных")
        details.events.forEach { Text("${it.minute} · ${it.teamName} · ${it.detail}") }
    }
}

@Composable
private fun AnalyticsTab(details: MatchDetails) {
    SectionCard("R-Match Analyst") {
        val analysis = details.analysis
        if (analysis == null) {
            Text("Недостаточно данных для расчёта")
            return@SectionCard
        }
        Text("Модель: ${analysis.modelName}")
        Text("Качество данных: ${analysis.dataQuality}")
        Text("Расчёт: ${analysis.calculatedAtIso}")
        Text("xG: ${"%.2f".format(analysis.expectedHomeGoals)} / ${"%.2f".format(analysis.expectedAwayGoals)}")
        Text("1X2")
        analysis.oneXTwo.forEach { Text("• ${it.label}: ${"%.1f".format(it.probability * 100)}%") }
        Text("Тоталы")
        analysis.totals.forEach { Text("• ${it.label}: ${"%.1f".format(it.probability * 100)}%") }
        Text("Обе забьют")
        analysis.bothTeamsToScore.forEach { Text("• ${it.label}: ${"%.1f".format(it.probability * 100)}%") }
        Text("Форы")
        analysis.handicaps.forEach { Text("• ${it.label}: ${"%.1f".format(it.probability * 100)}%") }
        Text("Факторы")
        analysis.factors.forEach { Text("• ${it.label}: ${it.impact}") }
        if (analysis.marketComparisons.isEmpty()) {
            Text("Коэффициенты недоступны или линия неполная/устаревшая")
        } else {
            Text("Сравнение с рынком")
            analysis.marketComparisons.forEach { Text("• ${it.label}: модель ${"%.1f".format(it.modelProbability * 100)}%, рынок ${"%.1f".format(it.normalizedMarketProbability * 100)}%, дельта ${"%.1f".format(it.delta * 100)} п.п.") }
        }
        Text(analysis.disclaimer, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LineupsTab(details: MatchDetails) {
    SectionCard("Составы") {
        if (details.lineups.isEmpty()) Text("Составы пока не опубликованы поставщиком")
        details.lineups.forEach { lineup ->
            Text("${lineup.team.name} · ${lineup.formation ?: "Схема не опубликована"}", fontWeight = FontWeight.Bold)
            lineup.coach?.let { Text("Тренер: ${it.name}") }
            Text("Стартовые XI")
            lineup.starting.forEach { Text("• ${it.number ?: 0} ${it.name} (${it.position ?: "—"}) ${it.grid ?: ""}") }
            Text("Запасные")
            lineup.bench.forEach { Text("• ${it.number ?: 0} ${it.name} (${it.position ?: "—"})") }
        }
        if (details.injuries.isNotEmpty()) {
            Text("Подтверждённые травмы")
            details.injuries.forEach { Text("• ${it.playerName}: ${it.type ?: "Травма"} · ${it.reason ?: "Причина не уточнена"}") }
        }
    }
}

@Composable
private fun StatisticsTab(details: MatchDetails) {
    SectionCard("Статистика") {
        if (details.statistics.isEmpty()) Text("Нет верифицированных данных")
        details.statistics.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.homeValue)
                Text(row.label, fontWeight = FontWeight.Medium)
                Text(row.awayValue)
            }
        }
    }
    SectionCard("Таблица") { details.standings.take(8).forEach { Text("${it.rank}. ${it.team.name} — ${it.points} очк. · форма ${it.form ?: "—"}") } }
}

@Composable
private fun OddsTab(details: MatchDetails) {
    SectionCard("Коэффициенты") {
        if (details.odds.isEmpty()) {
            Text("Коэффициенты недоступны")
            return@SectionCard
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(details.odds) { market ->
                SectionCard("${market.bookmaker} · ${market.market}") {
                    Text("Provider: API-Football / odds")
                    Text("Timestamp: ${market.updatedAtIso ?: "—"}")
                    Text("Freshness: ${if (market.isFresh) "актуально" else "устарело"}")
                    market.values.forEach { Text("• ${it.label}: ${it.decimal} (implied ${"%.1f".format(it.impliedProbability * 100)}%)") }
                }
            }
        }
        Text("Нейтральная формулировка: модель выше/ниже рынка показывается только для полной и свежей линии.")
        Text("Вероятности не гарантируют исход и не являются финансовой рекомендацией.", fontWeight = FontWeight.SemiBold)
    }
}
