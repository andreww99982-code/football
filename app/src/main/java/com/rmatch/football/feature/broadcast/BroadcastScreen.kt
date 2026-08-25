package com.rmatch.football.feature.broadcast

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmatch.football.core.network.ApiConstants
import com.rmatch.football.ui.components.SectionTitle
import com.rmatch.football.ui.theme.RMatchAccent
import com.rmatch.football.ui.theme.RMatchMuted
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

private data class BroadcasterLink(
    val name: String,
    val description: String,
    val url: String,
    val isFree: Boolean = false
)

private val OFFICIAL_BROADCASTERS = listOf(
    BroadcasterLink(
        name = "UEFA.tv",
        description = "Лига чемпионов, Лига Европы, Лига конференций — официальный стриминг УЕФА",
        url = "https://www.uefa.com/uefatv/",
        isFree = true
    ),
    BroadcasterLink(
        name = "FIFA+",
        description = "Чемпионат мира, Кубок конфедераций и другие турниры FIFA — бесплатный стриминг",
        url = "https://www.fifa.com/fifaplus/",
        isFree = true
    ),
    BroadcasterLink(
        name = "YouTube — UEFA",
        description = "Официальный канал UEFA: голы, обзоры матчей, пресс-конференции (бесплатно)",
        url = "https://www.youtube.com/@UEFA",
        isFree = true
    ),
    BroadcasterLink(
        name = "YouTube — LaLiga",
        description = "Официальный канал Ла Лиги: хайлайты и матчи (бесплатно)",
        url = "https://www.youtube.com/@LaLiga",
        isFree = true
    ),
    BroadcasterLink(
        name = "YouTube — Bundesliga",
        description = "Официальный канал Бундеслиги: лучшие голы и обзоры (бесплатно)",
        url = "https://www.youtube.com/@Bundesliga",
        isFree = true
    ),
    BroadcasterLink(
        name = "Match TV",
        description = "Официальный вещатель РПЛ и ряда европейских лиг на российском рынке",
        url = "https://matchtv.ru/"
    ),
    BroadcasterLink(
        name = "Premier League",
        description = "Расписание трансляций Английской Премьер-лиги на официальном сайте",
        url = "https://www.premierleague.com/broadcast-schedules"
    ),
    BroadcasterLink(
        name = "LaLiga",
        description = "Расписание трансляций Ла Лиги, доступные вещатели по регионам",
        url = "https://www.laliga.com/en-ES/laligasmartsports/broadcast"
    ),
    BroadcasterLink(
        name = "Bundesliga",
        description = "Официальный сайт Бундеслиги: расписание и список вещателей",
        url = "https://www.bundesliga.com/en/bundesliga"
    ),
    BroadcasterLink(
        name = "Serie A",
        description = "Официальный сайт Серии А: трансляции и расписание матчей",
        url = "https://www.legaseriea.it/en"
    ),
    BroadcasterLink(
        name = "Ligue 1",
        description = "Официальный сайт Лиги 1: список официальных вещателей по странам",
        url = "https://www.ligue1.com"
    ),
    BroadcasterLink(
        name = "ESPN+",
        description = "Трансляции Liga MX, LALIGA, Bundesliga, FA Cup и других турниров (США)",
        url = "https://www.espn.com/espnplus/"
    ),
    BroadcasterLink(
        name = "beIN Sports",
        description = "Международная сеть спортивных каналов: Лига чемпионов, Ла Лига и другие",
        url = "https://www.beinsports.com"
    )
)

@Composable
fun BroadcastScreen() {
    val context = LocalContext.current
    val free = OFFICIAL_BROADCASTERS.filter { it.isFree }
    val paid = OFFICIAL_BROADCASTERS.filterNot { it.isFree }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionTitle("Официальные источники трансляций")
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
            ) {
                Text(
                    text = "Приложение не воспроизводит и не размещает трансляции. " +
                        "Ниже представлены только официальные сайты правообладателей и " +
                        "лицензированных вещателей. Значок \uD83C\uDD13 означает бесплатный доступ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RMatchMuted,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionTitle("\uD83C\uDD13 Бесплатный доступ") }
        items(free, key = { it.url }) { link ->
            BroadcasterCard(link) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
        item { SectionTitle("Платные / региональные вещатели") }
        items(paid, key = { it.url }) { link ->
            BroadcasterCard(link) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = RMatchSurfaceVariant)
                Text(
                    text = ApiConstants.ATTRIBUTION,
                    style = MaterialTheme.typography.labelSmall,
                    color = RMatchMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BroadcasterCard(link: BroadcasterLink, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = RMatchSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = link.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = RMatchMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.OpenInBrowser,
                contentDescription = "Открыть в браузере",
                tint = RMatchAccent,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
