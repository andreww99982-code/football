package com.rmatch.football.feature.match_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.domain.analyst.AnalystResult
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.domain.model.Lineup
import com.rmatch.football.core.domain.usecase.MarketComparison
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.core.util.UiState
import com.rmatch.football.feature.analytics.AnalystReportView
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.RemoteLogo
import com.rmatch.football.ui.components.ResponsibleGamblingNote
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.components.StatusBadge
import com.rmatch.football.ui.components.formatOdd
import com.rmatch.football.ui.components.formatPercent
import com.rmatch.football.ui.components.formatSignedPercent
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

private val TABS = listOf("Обзор", "Аналитика", "Составы", "Статистика", "Коэффициенты")

@Composable
fun MatchDetailScreen(
    fixtureId: Int,
    onBack: () -> Unit,
    onOpenTeam: (Int) -> Unit,
    viewModel: MatchDetailViewModel = viewModel(
        key = "match-$fixtureId",
        factory = SimpleViewModelFactory {
            MatchDetailViewModel(
                ServiceLocator.repository,
                ServiceLocator.analyzeFixture,
                fixtureId
            )
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Матч") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            actions = {
                IconButton(onClick = viewModel::refreshCurrentTab) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить данные вкладки"
                    )
                }
            }
        )
        ScrollableTabRow(selectedTabIndex = state.selectedTab, edgePadding = 8.dp) {
            TABS.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { viewModel.onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (state.selectedTab) {
                0 -> OverviewTab(state)
                1 -> AnalyticsTab(state)
                2 -> LineupsTab(state)
                3 -> StatisticsTab(state)
                else -> OddsTab(state)
            }
        }

        val fixtureState = state.fixture
        AttributionFooter(
            fetchedAtMillis = (fixtureState as? UiState.Content)?.fetchedAtMillis,
            fromCache = (fixtureState as? UiState.Content)?.fromCache ?: false
        )
    }
}

@Composable
private fun OverviewTab(state: MatchDetailUiState) {
    when (val fixture = state.fixture) {
        is UiState.Loading -> LoadingState(items = 4)
        is UiState.Empty -> EmptyState(message = fixture.message)
        is UiState.Error -> ErrorState(message = fixture.message)
        is UiState.Content -> {
            val data = fixture.data
            FixtureHeader(data)
            SectionTitle("Информация о матче")
            InfoRow("Статус", data.status.description)
            InfoRow("Начало", TimeFormat.dateTimeMillis(data.timestampSeconds?.times(1000)))
            InfoRow("Лига", data.league?.name ?: "—")
            InfoRow("Тур", data.league?.round ?: "—")
            InfoRow("Стадион", data.venue?.name ?: "—")
            InfoRow("Город", data.venue?.city ?: "—")
            InfoRow("Судья", data.referee ?: ErrorMessages.NO_VERIFIED_DATA)

            SectionTitle("События")
            when (val events = state.events) {
                is UiState.Content -> events.data.forEach { event ->
                    InfoRow(
                        label = "${event.minute ?: "—"}' ${event.type ?: ""}",
                        value = listOfNotNull(event.playerName, event.detail).joinToString(" • ")
                            .ifBlank { "—" }
                    )
                }

                is UiState.Empty -> EmptyState(message = events.message)
                is UiState.Error -> ErrorState(message = events.message)
                is UiState.Loading -> LoadingState(items = 2)
            }

            if (state.injuries.isNotEmpty()) {
                SectionTitle("Травмы и потери (по данным поставщика)")
                state.injuries.forEach { injury ->
                    InfoRow(
                        label = injury.playerName,
                        value = listOfNotNull(injury.teamName, injury.reason)
                            .joinToString(" • ").ifBlank { "—" }
                    )
                }
            }
        }
    }
}

@Composable
private fun FixtureHeader(fixture: Fixture) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = RMatchSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteLogo(
                    url = fixture.home.logoUrl,
                    description = "Эмблема команды ${fixture.home.name}",
                    sizeDp = 36
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = fixture.home.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = fixture.scoreLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteLogo(
                    url = fixture.away.logoUrl,
                    description = "Эмблема команды ${fixture.away.name}",
                    sizeDp = 36
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = fixture.away.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    text = fixture.status.description,
                    live = fixture.status.isLive
                )
            }
        }
    }
}

@Composable
private fun AnalyticsTab(state: MatchDetailUiState) {
    when (val analysis = state.analysis) {
        is UiState.Loading -> LoadingState(items = 5)
        is UiState.Empty -> EmptyState(message = analysis.message)
        is UiState.Error -> ErrorState(message = analysis.message)
        is UiState.Content -> {
            val data = analysis.data
            when (val result = data.result) {
                is AnalystResult.Insufficient -> EmptyState(
                    message = result.reason,
                    hint = "Для расчёта нужно минимум 5 завершённых матчей у каждой команды."
                )

                is AnalystResult.Ready -> AnalystReportView(
                    report = result.report,
                    homeName = data.fixture.home.name,
                    awayName = data.fixture.away.name
                )
            }
        }
    }
}

@Composable
private fun LineupsTab(state: MatchDetailUiState) {
    when (val lineups = state.lineups) {
        is UiState.Loading -> LoadingState(items = 4)
        is UiState.Empty -> EmptyState(message = lineups.message)
        is UiState.Error -> ErrorState(message = lineups.message)
        is UiState.Content -> lineups.data.forEach { lineup -> LineupBlock(lineup) }
    }
}

@Composable
private fun LineupBlock(lineup: Lineup) {
    SectionTitle(lineup.team?.name ?: "Команда не указана")
    InfoRow("Схема", lineup.formation ?: "—")
    InfoRow("Тренер", lineup.coachName ?: "—")
    if (lineup.startXI.isEmpty()) {
        EmptyState(message = ErrorMessages.NO_LINEUPS)
    } else {
        SectionTitle("Стартовый состав")
        lineup.startXI.forEach { player ->
            InfoRow(
                label = "${player.number ?: "—"} ${player.name}",
                value = player.position ?: "—"
            )
        }
        if (lineup.substitutes.isNotEmpty()) {
            SectionTitle("Запасные")
            lineup.substitutes.forEach { player ->
                InfoRow(
                    label = "${player.number ?: "—"} ${player.name}",
                    value = player.position ?: "—"
                )
            }
        }
    }
}

@Composable
private fun StatisticsTab(state: MatchDetailUiState) {
    when (val statistics = state.statistics) {
        is UiState.Loading -> LoadingState(items = 4)
        is UiState.Empty -> EmptyState(message = statistics.message)
        is UiState.Error -> ErrorState(message = statistics.message)
        is UiState.Content -> statistics.data.forEach { block ->
            SectionTitle(block.team?.name ?: "Команда не указана")
            if (block.entries.isEmpty()) {
                EmptyState(message = ErrorMessages.NO_VERIFIED_DATA)
            } else {
                block.entries.forEach { entry -> InfoRow(entry.type, entry.value) }
            }
        }
    }
}

@Composable
private fun OddsTab(state: MatchDetailUiState) {
    when (val odds = state.odds) {
        is UiState.Loading -> LoadingState(items = 4)
        is UiState.Empty -> EmptyState(
            message = odds.message,
            hint = "Поставщик не публикует коэффициенты для этого матча или тариф их не включает."
        )

        is UiState.Error -> ErrorState(message = odds.message)
        is UiState.Content -> {
            Text(
                text = "Коэффициенты приводятся справочно: показаны источник, рынок, " +
                    "десятичный коэффициент, вероятность и свежесть данных.",
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchMuted,
                modifier = Modifier.padding(16.dp)
            )
            odds.data.forEach { comparison -> OddsCard(comparison) }
            ResponsibleGamblingNote()
        }
    }
}

@Composable
private fun OddsCard(comparison: MarketComparison) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = comparison.bookmakerName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Рынок: ${comparison.marketName} • источник: ${comparison.provider}",
                style = MaterialTheme.typography.labelSmall,
                color = RMatchMuted
            )
            Text(
                text = "Обновление линии: " +
                    TimeFormat.dateTimeMillis(comparison.updatedAtMillis) +
                    if (comparison.isStale) " • данные устарели" else " • данные свежие",
                style = MaterialTheme.typography.labelSmall,
                color = RMatchMuted
            )
            if (!comparison.isComplete) {
                Text(
                    text = "Линия неполная — нормализация и сравнение с моделью не выполняются.",
                    style = MaterialTheme.typography.labelSmall,
                    color = RMatchMuted
                )
            }
            Spacer(Modifier.height(8.dp))
            comparison.rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = formatOdd(row.decimalOdds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(64.dp)
                    )
                    Text(
                        text = row.impliedProbability?.let { formatPercent(it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        text = row.normalizedProbability?.let { formatPercent(it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        text = row.delta?.let { formatSignedPercent(it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = "Маржа букмекера: " +
                    (comparison.overround?.let { formatSignedPercent(it) } ?: "—") +
                    " • Дельта = модель − рынок (без маржи)",
                style = MaterialTheme.typography.labelSmall,
                color = RMatchMuted
            )
        }
    }
}
