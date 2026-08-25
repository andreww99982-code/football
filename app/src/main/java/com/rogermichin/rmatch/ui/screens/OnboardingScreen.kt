package com.rogermichin.rmatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(busy: Boolean, error: String?, onVerify: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("R-Match", style = MaterialTheme.typography.headlineMedium)
        Text("Roger&Michin Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Честный onboarding-режим: без API key приложение не показывает демо-данные и вымышленные матчи.")
                Text("Введите личный x-apisports-key от API-Football.com / API-Sports. Ключ проверяется реально и шифруется локально через EncryptedSharedPreferences.")
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { onVerify(key) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Проверяем…" else "Сохранить и проверить") }
                error?.let { Text("Ошибка: $it") }
            }
        }
    }
}
