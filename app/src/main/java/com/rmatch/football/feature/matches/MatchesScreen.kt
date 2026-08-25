package com.rmatch.football.feature.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.rmatch.football.core.domain.model.Fixture
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.core.util.UiState
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.FilterChipRow
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.OfflineBanner
import com.rmatch.football.ui.components.RemoteLogo
import com.rmatch.football.ui.components.StatusBadge
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface

@Composable
fun MatchesScreen(
    onOpenMatch: (Int) -> Unit,
    onOpenTeam: (Int) -> Unit,
    viewModel: MatchesViewModel = viewModel(
        factory = SimpleViewModelFactory {
            MatchesViewModel(ServiceLocator.repository, ServiceLocator.networkMonitor)
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Матчи") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface),
            actions = {
                IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить список матчей"
                    )
                }
            }
        )
        if (!state.online) OfflineBanner()

        FilterChipRow(
            options = MatchesFilter.entries.map { it.title },
            selectedIndex = MatchesFilter.entries.indexOf(state.filter),
            onSelect = { viewModel.onFilterSelected(MatchesFilter.entries[it]) }
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Поиск: команда, лига, страна") },
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Поиск")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (val content = state.content) {
            is UiState.Loading -> LoadingState(modifier = Modifier.fillMaxWidth())
            is UiState.Empty -> EmptyState(
                message = content.message,
                hint = "Поставщик не отдал матчей для выбранной даты."
            )

            is UiState.Error -> ErrorState(
                message = content.message,
                onRetry = { viewModel.refresh(forceRefresh = true) }
            )

            is UiState.Content -> {
                val filtered = MatchesGrouping.filter(content.data, state.query)
                if (filtered.isEmpty()) {
                    EmptyState(message = ErrorMessages.NO_VERIFIED_DATA)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        MatchesGrouping.groupByDate(filtered).forEach { (date, fixtures) ->
                            item(key = "header-$date") {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                            items(fixtures, key = { it.id }) { fixture ->
                                FixtureCard(
                                    fixture = fixture,
                                    onClick = { onOpenMatch(fixture.id) },
                                    onTeamClick = onOpenTeam
                                )
                            }
                        }
                    }
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
fun FixtureCard(
    fixture: Fixture,
    onClick: () -> Unit,
    onTeamClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = RMatchSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        fixture.league?.country,
                        fixture.league?.name
                    ).joinToString(" • ").ifBlank { "Лига не указана" },
                    style = MaterialTheme.typography.labelSmall,
                    color = RMatchMuted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    text = statusLabel(fixture),
                    live = fixture.status.isLive
                )
            }
            Spacer(Modifier.height(8.dp))
            TeamLine(
                name = fixture.home.name,
                logoUrl = fixture.home.logoUrl,
                goals = fixture.homeGoals,
                onClick = { onTeamClick(fixture.home.id) }
            )
            Spacer(Modifier.height(4.dp))
            TeamLine(
                name = fixture.away.name,
                logoUrl = fixture.away.logoUrl,
                goals = fixture.awayGoals,
                onClick = { onTeamClick(fixture.away.id) }
            )
        }
    }
}

@Composable
private fun TeamLine(
    name: String,
    logoUrl: String?,
    goals: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteLogo(url = logoUrl, description = "Эмблема команды $name")
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = goals?.toString() ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun statusLabel(fixture: Fixture): String = when {
    fixture.status.isLive -> "LIVE ${fixture.status.elapsedMinutes ?: 0}'"
    fixture.status.isFinished -> "Завершён • ${fixture.scoreLabel}"
    else -> TimeFormat.time(fixture.timestampSeconds)
}
