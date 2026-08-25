package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.MatchSummary
import com.rogermichin.rmatch.ui.MainUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MatchesScreen(state: MainUiState, onRefresh: (Boolean) -> Unit, onMatchClick: (MatchSummary) -> Unit, onCountryFilter: (String) -> Unit, onLeagueFilter: (String) -> Unit) {
    ScreenContainer("Матчи", "Ближайшие реальные матчи, группировка по дате и фильтрам", state.matches, { onRefresh(true) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.countryFilter, onValueChange = onCountryFilter, label = { Text("Фильтр по стране") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.leagueFilter, onValueChange = onLeagueFilter, label = { Text("Поиск по лиге/команде") }, modifier = Modifier.fillMaxWidth())
            val filtered = state.matches.value.orEmpty()
                .filter { state.countryFilter.isBlank() || it.country.contains(state.countryFilter, true) }
                .filter { state.leagueFilter.isBlank() || it.leagueName.contains(state.leagueFilter, true) || it.homeTeam.name.contains(state.leagueFilter, true) || it.awayTeam.name.contains(state.leagueFilter, true) }
            filtered.groupBy { formatDate(it.timestamp) }.forEach { (date, matches) ->
                Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                matches.forEach { match -> MatchCard(match, onMatchClick) }
            }
        }
    }
}

@Composable
fun MatchCard(match: MatchSummary, onClick: (MatchSummary) -> Unit) {
    SectionCard("${match.leagueName} · ${match.round ?: "Без раунда"}") {
        Column(modifier = Modifier.fillMaxWidth().clickable { onClick(match) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${match.homeTeam.name} — ${match.awayTeam.name}", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Статус: ${match.status}")
                Text("Счёт: ${if (match.homeGoals == null || match.awayGoals == null) "—" else "${match.homeGoals}:${match.awayGoals}"}")
            }
            Text("Стадион: ${match.venue ?: "Нет верифицированных данных"}")
            Text("Судья: ${match.referee ?: "Нет верифицированных данных"}")
        }
    }
}

private fun formatDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd MMMM, EEE").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(timestamp))
