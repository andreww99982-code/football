package com.rmatch.football.feature.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.rmatch.football.core.util.ErrorMessages
import com.rmatch.football.core.util.UiState
import com.rmatch.football.feature.matches.FixtureCard
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchSurface

@Composable
fun TeamScreen(
    teamId: Int,
    onBack: () -> Unit,
    onOpenMatch: (Int) -> Unit,
    onOpenPlayer: (Int) -> Unit,
    viewModel: TeamViewModel = viewModel(
        key = "team-$teamId",
        factory = SimpleViewModelFactory { TeamViewModel(ServiceLocator.repository, teamId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = (state.profile as? UiState.Content)?.data?.team?.name ?: "Команда"
                )
            },
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
                IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Обновить данные команды"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (val profile = state.profile) {
                is UiState.Loading -> LoadingState(items = 3)
                is UiState.Empty -> EmptyState(message = profile.message)
                is UiState.Error -> ErrorState(
                    message = profile.message,
                    onRetry = { viewModel.refresh(forceRefresh = true) }
                )

                is UiState.Content -> {
                    SectionTitle("Профиль")
                    InfoRow("Название", profile.data.team.name)
                    InfoRow("Страна", profile.data.country ?: "—")
                    InfoRow("Основан", profile.data.founded?.toString() ?: "—")
                    InfoRow("Стадион", profile.data.venue?.name ?: "—")
                    InfoRow("Вместимость", profile.data.venue?.capacity?.toString() ?: "—")
                }
            }

            SectionTitle("Форма (последние матчи)")
            InfoRow("Результаты", state.form.ifBlank { ErrorMessages.NO_VERIFIED_DATA })

            if (state.coaches.isNotEmpty()) {
                SectionTitle("Тренерский штаб")
                state.coaches.forEach { coach ->
                    InfoRow(coach.name, coach.nationality ?: "—")
                }
            }

            SectionTitle("Последние матчи")
            when (val last = state.lastMatches) {
                is UiState.Loading -> LoadingState(items = 3)
                is UiState.Empty -> EmptyState(message = last.message)
                is UiState.Error -> ErrorState(message = last.message)
                is UiState.Content -> last.data.forEach { fixture ->
                    FixtureCard(
                        fixture = fixture,
                        onClick = { onOpenMatch(fixture.id) },
                        onTeamClick = { }
                    )
                }
            }

            SectionTitle("Ближайшие матчи")
            when (val next = state.nextMatches) {
                is UiState.Loading -> LoadingState(items = 2)
                is UiState.Empty -> EmptyState(message = next.message)
                is UiState.Error -> ErrorState(message = next.message)
                is UiState.Content -> next.data.forEach { fixture ->
                    FixtureCard(
                        fixture = fixture,
                        onClick = { onOpenMatch(fixture.id) },
                        onTeamClick = { }
                    )
                }
            }

            SectionTitle("Состав")
            when (val squad = state.squad) {
                is UiState.Loading -> LoadingState(items = 4)
                is UiState.Empty -> EmptyState(message = squad.message)
                is UiState.Error -> ErrorState(message = squad.message)
                is UiState.Content -> squad.data.forEach { member ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { member.id?.let(onOpenPlayer) }
                            .padding(vertical = 2.dp)
                    ) {
                        InfoRow(
                            label = "${member.number ?: "—"} ${member.name}",
                            value = listOfNotNull(
                                member.position,
                                member.age?.let { "$it лет" }
                            ).joinToString(" • ").ifBlank { "—" }
                        )
                    }
                }
            }
        }

        AttributionFooter(
            fetchedAtMillis = (state.profile as? UiState.Content)?.fetchedAtMillis,
            fromCache = (state.profile as? UiState.Content)?.fromCache ?: false
        )
    }
}
