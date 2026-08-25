package com.rmatch.football.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.ui.components.AttributionFooter
import com.rmatch.football.ui.components.InfoRow
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurface
import com.rmatch.football.ui.theme.RMatchSurfaceVariant
import com.rmatch.football.ui.theme.RMatchWarning

@Composable
fun SettingsScreen(
    onKeyRemoved: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory {
            SettingsViewModel(
                repository = ServiceLocator.repository,
                keyStorage = ServiceLocator.apiKeyStorage,
                networkMonitor = ServiceLocator.networkMonitor
            )
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.keyRemoved) {
        if (state.keyRemoved) onKeyRemoved()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Настройки") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RMatchSurface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Ключ API")
            InfoRow("Статус", if (state.hasKey) "Сохранён" else "Не сохранён")
            InfoRow("Ключ", state.maskedKey)
            Text(
                text = "Ключ хранится только на устройстве в EncryptedSharedPreferences и " +
                    "никогда не попадает в исходный код, логи или репозиторий.",
                style = MaterialTheme.typography.bodySmall,
                color = RMatchMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SectionTitle("Квота провайдера")
            InfoRow("Осталось запросов", state.quota.remaining?.toString() ?: "—")
            InfoRow("Лимит по заголовкам", state.quota.limit?.toString() ?: "—")
            InfoRow(
                "Обновлено",
                state.quota.updatedAtMillis?.let { TimeFormat.dateTimeMillis(it) } ?: "—"
            )
            InfoRow("Тариф", state.status?.plan ?: "—")
            InfoRow("Запросов сегодня", state.status?.requestsToday?.toString() ?: "—")
            InfoRow("Лимит в сутки", state.status?.requestsLimitPerDay?.toString() ?: "—")
            InfoRow("Подписка активна до", state.status?.subscriptionEnd ?: "—")

            SectionTitle("Соединение")
            InfoRow("Сеть", if (state.online) "Доступна" else "Нет соединения")
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Button(
                onClick = viewModel::checkConnection,
                enabled = !state.checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (state.checking) "Проверяем…" else "Проверить соединение и квоту")
            }

            SectionTitle("Смена ключа")
            OutlinedTextField(
                value = state.newKeyInput,
                onValueChange = viewModel::onNewKeyChanged,
                label = { Text("Новый ключ x-apisports-key") },
                singleLine = true,
                isError = state.newKeyError != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            state.newKeyError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = RMatchWarning,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Button(
                onClick = viewModel::saveNewKey,
                enabled = !state.savingKey && state.newKeyInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (state.savingKey) "Проверяем ключ…" else "Сохранить новый ключ")
            }

            SectionTitle("Кэш")
            InfoRow("Записей в кэше", state.cachedEntries.toString())
            InfoRow(
                "Последнее обновление",
                state.lastCacheUpdate?.let { TimeFormat.dateTimeMillis(it) } ?: "—"
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = viewModel::clearCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Очистить кэш")
                }
            }

            SectionTitle("Удаление ключа")
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = state.hasKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Удалить ключ и очистить кэш")
            }

            SectionTitle("Продакшн-предупреждение")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
            ) {
                Text(
                    text = "В продакшене ключ провайдера не должен храниться на устройстве. " +
                        "Используйте собственный backend-прокси: приложение обращается к вашему " +
                        "серверу, а сервер добавляет ключ и контролирует лимиты. Текущий режим " +
                        "с локальным ключом предназначен для личного использования.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            SectionTitle("Источники и правила")
            Text(
                text = "Данные предоставляются API-Football.com (API-Sports). Приложение не " +
                    "парсит сторонние сайты, не обходит ограничения провайдеров и не " +
                    "распространяет нелегальные трансляции.",
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        AttributionFooter(fetchedAtMillis = state.lastCacheUpdate, fromCache = false)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить ключ?") },
            text = {
                Text(
                    "Ключ будет удалён из защищённого хранилища, кэш очищен. " +
                        "Приложение вернётся к экрану ввода ключа."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteKey()
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
