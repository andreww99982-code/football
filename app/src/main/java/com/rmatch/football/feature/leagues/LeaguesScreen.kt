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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.RemoteLogo
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface

@Composable
fun LeaguesScreen(
    onOpenLeague: (Int, Int) -> Unit,
    viewModel: LeaguesViewModel = viewModel(
        factory = SimpleViewModelFactory { LeaguesViewModel(ServiceLocator.repository) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Лиги • сезон ${state.season}") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface),
            actions = {
                IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить список лиг"
                    )
                }
            }
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Поиск лиги или страны") },
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Поиск лиги")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (val content = state.content) {
            is UiState.Loading -> LoadingState()
            is UiState.Empty -> EmptyState(message = content.message)
            is UiState.Error -> ErrorState(
                message = content.message,
                onRetry = { viewModel.refresh(forceRefresh = true) }
            )

            is UiState.Content -> {
                val filtered = LeagueFiltering.filter(content.data, state.query)
                if (filtered.isEmpty()) {
                    EmptyState(message = ErrorMessages.NO_VERIFIED_DATA)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        LeagueFiltering.groupByCountry(filtered).forEach { (country, leagues) ->
                            item(key = "country-$country") {
                                Text(
                                    text = country,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                            items(leagues, key = { it.league.id }) { league ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenLeague(
                                                league.league.id,
                                                league.currentSeason ?: state.season
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RemoteLogo(
                                        url = league.league.logoUrl,
                                        description = "Логотип лиги ${league.league.name}"
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = league.league.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Сезон: " +
                                                (league.currentSeason?.toString() ?: "—"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RMatchMuted
                                        )
                                    }
                                }
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
