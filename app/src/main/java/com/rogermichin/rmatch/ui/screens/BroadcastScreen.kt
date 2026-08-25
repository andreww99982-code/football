package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogermichin.rmatch.data.ScreenData

@Composable
fun BroadcastScreen() {
    ScreenContainer("Эфир", "Только официальные будущие ссылки правообладателей", ScreenData(value = Unit), null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("Официальные трансляции") {
                Text("R-Match не добавляет scraping, IPTV, пиратские плееры и неофициальные ссылки.", fontWeight = FontWeight.SemiBold)
                Text("Если официальный licensed provider появится в API или production backend proxy, здесь будут показаны только подтверждённые правообладатели.")
                Text("Сейчас: Нет верифицированных данных")
            }
        }
    }
}
