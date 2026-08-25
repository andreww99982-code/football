package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.LeagueSummary
import com.rogermichin.rmatch.ui.MainUiState

@Composable
fun LeaguesScreen(state: MainUiState, onRefresh: (Boolean) -> Unit, onLeagueClick: (LeagueSummary) -> Unit) {
    ScreenContainer("Лиги", "Реальные турниры и доступные покрытия тарифа API-Football", state.leagues, { onRefresh(true) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.leagues.value.orEmpty().forEach { league ->
                SectionCard(league.name) {
                    Column(modifier = Modifier.fillMaxWidth().clickable { onLeagueClick(league) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${league.country} · ${league.type}", fontWeight = FontWeight.Medium)
                        Text("Сезон: ${league.season}")
                        Text("Таблица: ${if (league.standingsSupported) "доступна" else "нет"}")
                        Text("Odds API: ${if (league.oddsSupported) "доступен" else "может быть недоступен тарифу"}")
                    }
                }
            }
        }
    }
}
