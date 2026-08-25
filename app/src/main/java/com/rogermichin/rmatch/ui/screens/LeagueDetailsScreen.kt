package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.LeagueDetails
import com.rogermichin.rmatch.data.MatchSummary
import com.rogermichin.rmatch.data.ScreenData

@Composable
fun LeagueDetailsScreen(state: ScreenData<LeagueDetails>, onBack: () -> Unit, onMatchClick: (MatchSummary) -> Unit, onTeamClick: (Int, Int, Int) -> Unit) {
    ScreenContainer(state.value?.league?.name ?: "Лига", "Таблица и ближайшие матчи", state, onBack) {
        state.value?.let { details ->
            SectionCard("Турнирная таблица") {
                if (details.standings.isEmpty()) Text("Нет верифицированных данных")
                details.standings.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${row.rank}. ${row.team.name}", modifier = Modifier.weight(1f).clickable { onTeamClick(row.team.id, details.league.id, details.league.season) }, fontWeight = FontWeight.Medium)
                        Text("${row.points} очк.")
                    }
                }
            }
            SectionCard("Матчи") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { details.fixtures.forEach { MatchCard(it, onMatchClick) } } }
        }
    }
}
