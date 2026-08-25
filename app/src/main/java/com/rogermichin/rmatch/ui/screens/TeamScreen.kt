package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.ScreenData
import com.rogermichin.rmatch.data.TeamProfile

@Composable
fun TeamScreen(state: ScreenData<TeamProfile>, onBack: () -> Unit) {
    ScreenContainer(state.value?.team?.name ?: "Команда", "Форма, состав, тренеры и статистика", state, onBack) {
        state.value?.let { team ->
            SectionCard("Профиль") {
                Text("Страна: ${team.country ?: "Нет верифицированных данных"}")
                Text("Основан: ${team.founded ?: 0}")
                Text("Стадион: ${team.venue ?: "Нет верифицированных данных"}")
                Text("Город: ${team.city ?: "Нет верифицированных данных"}")
            }
            SectionCard("Статистика") {
                team.statistics?.let {
                    Text("Форма: ${it.form ?: "—"}")
                    Text("Матчи дома/в гостях: ${it.playedHome ?: 0}/${it.playedAway ?: 0}")
                    Text("Голы дома: ${it.goalsForHomeAvg ?: 0.0} · пропущено дома: ${it.goalsAgainstHomeAvg ?: 0.0}")
                    Text("Голы в гостях: ${it.goalsForAwayAvg ?: 0.0} · пропущено в гостях: ${it.goalsAgainstAwayAvg ?: 0.0}")
                    Text("Сухие матчи: ${it.cleanSheets ?: 0} · не забили: ${it.failedToScore ?: 0}")
                } ?: Text("Нет верифицированных данных")
            }
            SectionCard("Тренеры") { if (team.coachs.isEmpty()) Text("Нет верифицированных данных"); team.coachs.forEach { Text("• ${it.name} · ${it.nationality ?: "—"}") } }
            SectionCard("Состав") { if (team.squad.isEmpty()) Text("Нет верифицированных данных"); team.squad.forEach { Text("• ${it.number ?: 0} ${it.name} · ${it.position ?: "—"} · ${it.goals ?: 0}G/${it.assists ?: 0}A") } }
            SectionCard("Последние матчи") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { team.recentMatches.forEach { Text("• ${it.homeTeam.name} ${it.homeGoals ?: "—"}:${it.awayGoals ?: "—"} ${it.awayTeam.name}") } } }
            SectionCard("Травмы") { if (team.injuries.isEmpty()) Text("Нет верифицированных данных"); team.injuries.forEach { Text("• ${it.playerName} · ${it.type ?: "—"} · ${it.reason ?: "Причина не уточнена"}") } }
            Text("© Roger&Michin Studio", fontWeight = FontWeight.SemiBold)
        }
    }
}
