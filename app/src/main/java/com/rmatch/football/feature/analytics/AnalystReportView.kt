package com.rmatch.football.feature.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rmatch.football.core.domain.analyst.AnalystReport
import com.rmatch.football.core.domain.analyst.PoissonCalculator
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.ProbabilityBar
import com.rmatch.football.ui.components.ResponsibleGamblingNote
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSecondary
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

/** Renders a finished R-Match Analyst report. Neutral wording only. */
@Composable
fun AnalystReportView(
    report: AnalystReport,
    homeName: String,
    awayName: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle("Вероятности исхода (1X2)")
        ProbabilityBar("Победа: $homeName", report.outcome.homeWin)
        ProbabilityBar("Ничья", report.outcome.draw)
        ProbabilityBar("Победа: $awayName", report.outcome.awayWin)

        SectionTitle("Ожидаемые голы (модель Пуассона)")
        InfoRow("xG $homeName", PoissonCalculator.roundTo(report.expectedGoalsHome, 2).toString())
        InfoRow("xG $awayName", PoissonCalculator.roundTo(report.expectedGoalsAway, 2).toString())
        InfoRow("Всего голов", PoissonCalculator.roundTo(report.expectedTotalGoals, 2).toString())

        SectionTitle("Тоталы")
        report.totals.forEach { total ->
            ProbabilityBar("Больше ${total.line}", total.over, color = RMatchSecondary)
            ProbabilityBar("Меньше ${total.line}", total.under)
        }

        SectionTitle("Обе забьют")
        ProbabilityBar("Да", report.bttsYes, color = RMatchSecondary)
        ProbabilityBar("Нет", report.bttsNo)

        SectionTitle("Форы (на хозяев)")
        report.handicaps.forEach { handicap ->
            val label = if (handicap.line > 0) "+${handicap.line}" else handicap.line.toString()
            ProbabilityBar("Фора $label — проходит", handicap.homeCovers)
        }

        SectionTitle("Как считалось")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = report.methodology,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                report.factors.forEach { factor ->
                    Text(
                        text = "• $factor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RMatchMuted
                    )
                }
            }
        }

        InfoRow("Матчей в расчёте (хозяева)", report.matchesUsedHome.toString())
        InfoRow("Матчей в расчёте (гости)", report.matchesUsedAway.toString())
        InfoRow("Качество данных", "${report.dataQuality.score} / 100")
        InfoRow("Время расчёта", "${report.computationMillis} мс")
        InfoRow("Расчёт выполнен", TimeFormat.dateTimeMillis(report.computedAtMillis))

        SectionTitle("Качество данных")
        report.dataQuality.notes.forEach { note ->
            Text(
                text = "• $note",
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        SectionTitle("Риски и ограничения")
        report.risks.forEach { risk ->
            Text(
                text = "• $risk",
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        ResponsibleGamblingNote()
    }
}
