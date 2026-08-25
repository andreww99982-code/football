package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.MatchSummary
import com.rogermichin.rmatch.ui.MainUiState

@Composable
fun AnalyticsScreen(state: MainUiState, onRefresh: (Boolean) -> Unit, onMatchClick: (MatchSummary) -> Unit) {
    ScreenContainer("Аналитика", "Детерминированная модель на реальных матчах API-Football", state.analytics, { onRefresh(true) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.analytics.value.orEmpty().forEach { (match, analysis) ->
                SectionCard("${match.homeTeam.name} — ${match.awayTeam.name}") {
                    MatchCard(match, onMatchClick)
                    if (analysis == null) {
                        Text("Недостаточно данных для расчёта")
                    } else {
                        Text("1X2: ${analysis.oneXTwo.joinToString { "${it.label} ${"%.0f".format(it.probability * 100)}%" }}", fontWeight = FontWeight.Medium)
                        Text("xG: ${"%.2f".format(analysis.expectedHomeGoals)} / ${"%.2f".format(analysis.expectedAwayGoals)}")
                        Text("Риски: ${analysis.dataQuality}; ${analysis.disclaimer}")
                    }
                }
            }
        }
    }
}
