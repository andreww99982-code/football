package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.ui.MainUiState

@Composable
fun SettingsScreen(state: MainUiState, onRefresh: () -> Unit, onVerifyNewKey: (String) -> Unit, onDeleteKey: () -> Unit, onClearCache: () -> Unit) {
    var newKey by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    ScreenContainer("Настройки", "Ключ хранится локально, шифруется и никогда не попадает в Git, BuildConfig, Room, README, CI и логи", state.apiHealth, onRefresh) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("API") {
                Text("Статус: ${if (state.apiHealth.value?.active == true) "активен" else "проверьте ключ"}")
                Text("Тариф: ${state.apiHealth.value?.subscriptionPlan ?: "Нет верифицированных данных"}")
                Text("Ключ: ${state.maskedApiKey}")
                Text("Проверено: ${state.apiHealth.value?.checkedAtIso ?: "—"}")
            }
            SectionCard("Квота") {
                TwoColumn("Лимит запросов", state.quota.requestsLimit?.toString() ?: "—")
                TwoColumn("Осталось", state.quota.requestsRemaining?.toString() ?: "—")
                TwoColumn("Лимит в день", state.quota.dailyLimit?.toString() ?: "—")
                TwoColumn("Использовано сегодня", state.quota.usedToday?.toString() ?: "—")
                Text("Если headers недоступны на тарифе, значения останутся пустыми.")
            }
            SectionCard("Смена ключа") {
                OutlinedTextField(value = newKey, onValueChange = { newKey = it }, label = { Text("Новый API key") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onVerifyNewKey(newKey) }, modifier = Modifier.fillMaxWidth()) { Text("Проверить и сохранить") }
            }
            SectionCard("Хранилище") {
                Button(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) { Text("Очистить кэш") }
                Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Удалить ключ и очистить кэш") }
                Text("Production warning: для production лучше использовать backend proxy вместо прямого ключа на клиенте.", fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить ключ?") },
            text = { Text("Ключ будет удалён, кэш очищен, приложение вернётся в onboarding-режим.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteKey() }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}
