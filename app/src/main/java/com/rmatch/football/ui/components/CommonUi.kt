package com.rmatch.football.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rmatch.football.core.network.ApiConstants
import com.rmatch.football.core.util.TimeFormat
import com.rmatch.football.ui.theme.RMatchAccent
import com.rmatch.football.ui.theme.RMatchLive
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = RMatchAccent,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RemoteLogo(
    url: String?,
    description: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 28
) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RMatchSurfaceVariant)
                .semantics { contentDescription = description }
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(sizeDp.dp)
        )
    }
}

@Composable
fun StatusBadge(text: String, live: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (live) RMatchLive.copy(alpha = 0.18f) else RMatchSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (live) RMatchLive else RMatchMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                color = if (selected) RMatchAccent.copy(alpha = 0.18f) else RMatchSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .semantics { contentDescription = "Фильтр: $label" }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) RMatchAccent else RMatchMuted,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ProbabilityBar(
    label: String,
    probability: Double,
    modifier: Modifier = Modifier,
    color: Color = RMatchAccent
) {
    val safe = probability.coerceIn(0.0, 1.0)
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatPercent(safe),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RMatchSurfaceVariant)
                .semantics { contentDescription = "$label: ${formatPercent(safe)}" }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safe.toFloat())
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

fun formatPercent(value: Double): String {
    val percent = (value.coerceIn(0.0, 1.0) * 1000).toInt() / 10.0
    return "$percent%"
}

fun formatSignedPercent(value: Double): String {
    val percent = (value * 1000).toInt() / 10.0
    return if (percent > 0) "+$percent%" else "$percent%"
}

fun formatOdd(value: Double?): String =
    value?.let { ((it * 100).toInt() / 100.0).toString() } ?: "—"

@Composable
fun ResponsibleGamblingNote(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
    ) {
        Text(
            text = "Информация носит справочный характер и не является советом или призывом " +
                "к участию в азартных играх. Играйте ответственно: 18+. " +
                "Никакой прогноз не гарантирует результат.",
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun AttributionFooter(
    fetchedAtMillis: Long?,
    fromCache: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = RMatchSurfaceVariant)
        Text(
            text = ApiConstants.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = RMatchMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Text(
            text = buildString {
                append("Обновлено: ")
                append(TimeFormat.dateTimeMillis(fetchedAtMillis))
                if (fromCache) append(" • данные из кэша")
            },
            style = MaterialTheme.typography.labelSmall,
            color = RMatchMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
