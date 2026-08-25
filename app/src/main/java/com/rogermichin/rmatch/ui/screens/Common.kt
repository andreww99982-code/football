package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.DataMeta
import com.rogermichin.rmatch.data.ScreenData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScreenContainer(title: String, subtitle: String? = null, state: ScreenData<*>, onRefresh: (() -> Unit)? = null, content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        item { MetaCard(state.meta, state.error, state.emptyMessage, state.loading, onRefresh) }
        if (!state.loading) item { content() }
    }
}

@Composable
fun MetaCard(meta: DataMeta?, error: String?, emptyMessage: String?, loading: Boolean, onRefresh: (() -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text("Загрузка верифицированных данных…")
                }
            }
            meta?.let {
                AssistChip(onClick = {}, label = { Text("Источник: ${it.source}") })
                Text("Обновлено: ${DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.fetchedAtEpochMillis))}")
                if (it.stale) Text("Показан устаревший кэш: сеть или квота недоступны")
            }
            error?.let { Text("Ошибка: $it") }
            if (!loading && error == null && !emptyMessage.isNullOrBlank()) Text(emptyMessage)
            onRefresh?.let { OutlinedButton(onClick = it) { Text("Обновить") } }
        }
    }
}

@Composable
fun SectionCard(title: String, body: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            body()
        }
    }
}

@Composable
fun TwoColumn(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}
