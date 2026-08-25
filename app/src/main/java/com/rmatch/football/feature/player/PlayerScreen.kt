package com.rmatch.football.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.util.UiState
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.EmptyState
import com.rmatch.football.ui.components.ErrorState
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.LoadingState
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchSurface

@Composable
fun PlayerScreen(
    playerId: Int,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(
        key = "player-$playerId",
        factory = SimpleViewModelFactory { PlayerViewModel(ServiceLocator.repository, playerId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(text = (state.content as? UiState.Content)?.data?.name ?: "Игрок")
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
                        contentDescription = "Обновить данные игрока"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (val content = state.content) {
                is UiState.Loading -> LoadingState(items = 4)
                is UiState.Empty -> EmptyState(message = content.message)
                is UiState.Error -> ErrorState(
                    message = content.message,
                    onRetry = { viewModel.refresh(forceRefresh = true) }
                )

                is UiState.Content -> {
                    val player = content.data
                    SectionTitle("Профиль игрока")
                    InfoRow("Имя", player.name)
                    InfoRow("Полное имя", listOfNotNull(player.firstName, player.lastName)
                        .joinToString(" ").ifBlank { "—" })
                    InfoRow("Возраст", player.age?.toString() ?: "—")
                    InfoRow("Дата рождения", player.birthDate ?: "—")
                    InfoRow("Место рождения", player.birthPlace ?: "—")
                    InfoRow("Гражданство", player.nationality ?: "—")
                    InfoRow("Рост", player.height ?: "—")
                    InfoRow("Вес", player.weight ?: "—")
                    InfoRow(
                        "Травмирован",
                        when (player.injured) {
                            true -> "Да"
                            false -> "Нет"
                            null -> "—"
                        }
                    )

                    SectionTitle("Статистика сезона ${state.season}")
                    if (player.seasons.isEmpty()) {
                        EmptyState(message = "Нет верифицированных данных")
                    } else {
                        player.seasons.forEach { season ->
                            SectionTitle(
                                listOfNotNull(season.teamName, season.leagueName)
                                    .joinToString(" • ").ifBlank { "—" }
                            )
                            InfoRow("Матчи", season.appearances?.toString() ?: "—")
                            InfoRow("В старте", season.lineups?.toString() ?: "—")
                            InfoRow("Минуты", season.minutes?.toString() ?: "—")
                            InfoRow("Позиция", season.position ?: "—")
                            InfoRow("Рейтинг", season.rating ?: "—")
                            InfoRow("Голы", season.goals?.toString() ?: "—")
                            InfoRow("Передачи", season.assists?.toString() ?: "—")
                            InfoRow("ЖК / КК", "${season.yellowCards ?: "—"} / ${season.redCards ?: "—"}")
                        }
                    }
                }
            }
        }

        AttributionFooter(
            fetchedAtMillis = (state.content as? UiState.Content)?.fetchedAtMillis,
            fromCache = (state.content as? UiState.Content)?.fromCache ?: false
        )
    }
}
