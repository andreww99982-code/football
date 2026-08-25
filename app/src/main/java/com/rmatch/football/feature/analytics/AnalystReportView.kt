package com.rmatch.football.feature.analytics

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmatch.football.core.domain.analyst.AnalystReport
import com.rmatch.football.core.domain.analyst.BettingRecommendation
import com.rmatch.football.core.domain.analyst.PoissonCalculator
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.ProbabilityBar
import com.rmatch.football.ui.components.ResponsibleGamblingNote
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchAccent
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchOnDark
import com.rmatch.football.ui.theme.RMatchSecondary
import com.rmatch.football.ui.theme.RMatchSurface
import com.rmatch.football.ui.theme.RMatchSurfaceVariant
import com.rmatch.football.ui.theme.RMatchWarning

/** Renders a finished R-Match Analyst report. Neutral wording only. */
@Composable
fun AnalystReportView(
    report: AnalystReport,
    homeName: String,
    awayName: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Recommendation banner ──────────────────────────────────────────
        report.recommendation?.let { rec ->
            RecommendationBanner(rec)
        }

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

@Composable
private fun RecommendationBanner(rec: BettingRecommendation) {
    val confidenceColor = when (rec.confidence) {
        "Высокая" -> RMatchAccent
        "Средняя" -> RMatchWarning
        else -> RMatchMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = RMatchSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.5.dp,
                    color = confidenceColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Рекомендация",
                    tint = confidenceColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Рекомендация модели",
                    style = MaterialTheme.typography.titleMedium,
                    color = confidenceColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Main outcome headline
            Text(
                text = rec.outcomeLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = RMatchOnDark,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(4.dp))

            // Probability bar inline
            ProbabilityBar(
                label = "Вероятность: ${
                    PoissonCalculator.roundTo(
                        rec.probability * 100,
                        1
                    )
                }%",
                probability = rec.probability,
                color = confidenceColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Уверенность: ${rec.confidence}",
                style = MaterialTheme.typography.labelMedium,
                color = confidenceColor
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = confidenceColor.copy(alpha = 0.2f)
            )

            // Reasoning bullets
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Обоснование",
                    tint = RMatchMuted,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Почему именно этот исход:",
                    style = MaterialTheme.typography.labelMedium,
                    color = RMatchMuted
                )
            }
            Spacer(Modifier.height(6.dp))
            rec.reasoning.forEach { point ->
                Text(
                    text = "• $point",
                    style = MaterialTheme.typography.bodySmall,
                    color = RMatchOnDark,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
