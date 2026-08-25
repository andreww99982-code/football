package com.rmatch.football.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.core.di.SimpleViewModelFactory
import com.rmatch.football.core.network.ApiConstants
import com.rmatch.football.ui.components.ResponsibleGamblingNote
import com.rmatch.football.ui.theme.RMatchError
import com.rmatch.football.ui.theme.RMatchMuted

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(
        factory = SimpleViewModelFactory { OnboardingViewModel(ServiceLocator.repository) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "R-Match",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "by Roger&Michin Studio",
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Приложение работает только с официальным API-Football (API-Sports). " +
                "Введите личный API-ключ — он сохраняется в зашифрованном хранилище устройства " +
                "и не передаётся никуда, кроме ${ApiConstants.PROVIDER_URL}",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Где взять ключ: зарегистрируйтесь на api-football.com (или на dashboard.api-football.com), " +
                "откройте раздел с профилем и скопируйте значение API Key. " +
                "Бесплатный тариф обычно даёт 100 запросов в сутки — приложение кэширует ответы, " +
                "чтобы экономить лимит.",
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.key,
            onValueChange = viewModel::onKeyChanged,
            label = { Text("API-ключ") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchError
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !state.checking,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.checking) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
            } else {
                Text("Проверить и сохранить")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Ключ проверяется запросом к /status. Приложение не содержит демо-данных: " +
                "без валидного ключа матчи и статистика не отображаются.",
            style = MaterialTheme.typography.bodyMedium,
            color = RMatchMuted
        )
        ResponsibleGamblingNote()
        Text(
            text = ApiConstants.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = RMatchMuted
        )
    }
}
