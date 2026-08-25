package com.rmatch.football.feature.leagues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.rmatch.football.core.util.UiState
import com.rmatch.football.feature.matches.FixtureCard
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.RemoteLogo
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface

@Composable
fun LeagueDetailScreen(
    leagueId: Int,
    season: Int,
    onBack: () -> Unit,
    onOpenMatch: (Int) -> Unit,
    onOpenTeam: (Int) -> Unit,
    viewModel: LeagueDetailViewModel = viewModel(
        key = "league-$leagueId-$season",
        factory = SimpleViewModelFactory {
            LeagueDetailViewModel(ServiceLocator.repository, leagueId, season)
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf("Таблица", "Матчи")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Лига $leagueId • сезон $season") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }
        )
        TabRow(selectedTabIndex = state.selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { viewModel.onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        if (state.selectedTab == 0) {
            when (val standings = state.standings) {
                is UiState.Loading -> LoadingState()
                is UiState.Empty -> EmptyState(message = standings.message)
                is UiState.Error -> ErrorState(
                    message = standings.message,
                    onRetry = { viewModel.refresh(forceRefresh = true) }
                )

                is UiState.Content -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item(key = "standings-header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RMatchMuted,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = "Команда",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RMatchMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "И",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RMatchMuted,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = "Р",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RMatchMuted,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = "О",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RMatchMuted,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                        items(standings.data, key = { it.team.id }) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTeam(row.team.id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.rank.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(28.dp)
                                )
                                RemoteLogo(
                                    url = row.team.logoUrl,
                                    description = "Эмблема команды ${row.team.name}",
                                    sizeDp = 22
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = row.team.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = row.played.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = row.goalsDiff.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = row.points.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                    }
                    AttributionFooter(
                        fetchedAtMillis = standings.fetchedAtMillis,
                        fromCache = standings.fromCache
                    )
                }
            }
        } else {
            when (val fixtures = state.fixtures) {
                is UiState.Loading -> LoadingState()
                is UiState.Empty -> EmptyState(message = fixtures.message)
                is UiState.Error -> ErrorState(
                    message = fixtures.message,
                    onRetry = { viewModel.refresh(forceRefresh = true) }
                )

                is UiState.Content -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(fixtures.data, key = { it.id }) { fixture ->
                            FixtureCard(
                                fixture = fixture,
                                onClick = { onOpenMatch(fixture.id) },
                                onTeamClick = onOpenTeam
                            )
                        }
                    }
                    AttributionFooter(
                        fetchedAtMillis = fixtures.fetchedAtMillis,
                        fromCache = fixtures.fromCache
                    )
                }
            }
        }
    }
}
