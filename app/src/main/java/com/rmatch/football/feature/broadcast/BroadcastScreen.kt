package com.rmatch.football.feature.broadcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rmatch.football.core.network.ApiConstants
import com.rmatch.football.ui.theme.RMatchAccent
import com.rmatch.football.ui.theme.RMatchMuted

@Composable
fun BroadcastScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.LiveTv,
            contentDescription = "Раздел Эфир",
            tint = RMatchAccent
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Эфир",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Приложение не размещает и не воспроизводит пиратские трансляции. " +
                "В будущем здесь появятся только официальные ссылки правообладателей " +
                "и телевещателей, если такие данные будут доступны легально.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Сейчас официальных ссылок для показа нет.",
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = ApiConstants.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = RMatchMuted
        )
    }
}
