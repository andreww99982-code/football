package com.rmatch.football.feature.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.domain.analyst.MatchAnalyst
import com.rmatch.football.core.util.UiState
import com.rmatch.football.feature.matches.FixtureCard
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.FilterChipRow
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.ResponsibleGamblingNote
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

@Composable
fun AnalyticsScreen(
    onOpenMatch: (Int) -> Unit,
    viewModel: AnalyticsViewModel = viewModel(
        factory = SimpleViewModelFactory { AnalyticsViewModel(ServiceLocator.repository) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val options = listOf("Сегодня", "Завтра", "Послезавтра")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("R-Match Analyst") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface),
            actions = {
                IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить список матчей для анализа"
                    )
                }
            }
        )
        FilterChipRow(
            options = options,
            selectedIndex = state.dayOffset,
            onSelect = viewModel::onDayOffsetSelected
        )

        when (val content = state.content) {
            is UiState.Loading -> LoadingState()
            is UiState.Empty -> EmptyState(
                message = content.message,
                hint = "Выберите другую дату — расчёт делается только по реальным матчам."
            )

            is UiState.Error -> ErrorState(
                message = content.message,
                onRetry = { viewModel.refresh(forceRefresh = true) }
            )

            is UiState.Content -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item(key = "methodology") { MethodologyCard() }
                    val upcoming = content.data.filter { !it.status.isFinished }
                    items(upcoming, key = { it.id }) { fixture ->
                        FixtureCard(
                            fixture = fixture,
                            onClick = { onOpenMatch(fixture.id) },
                            onTeamClick = { }
                        )
                    }
                    item(key = "disclaimer") { ResponsibleGamblingNote() }
                }
                AttributionFooter(
                    fetchedAtMillis = content.fetchedAtMillis,
                    fromCache = content.fromCache
                )
            }
        }
    }
}

@Composable
private fun MethodologyCard() {
    Column {
        SectionTitle("Методика расчёта")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = MatchAnalyst.METHODOLOGY,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Минимум ${MatchAnalyst.MIN_MATCHES} завершённых матчей у каждой " +
                        "команды, максимум ${MatchAnalyst.MAX_MATCHES} последних матчей в выборке. " +
                        "Если данных меньше — расчёт не выполняется.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RMatchMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Откройте матч и перейдите на вкладку «Аналитика», чтобы увидеть расчёт.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RMatchMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
