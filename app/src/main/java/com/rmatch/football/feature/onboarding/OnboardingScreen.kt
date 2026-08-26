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
import androidx.compose.material3.OutlinedButton
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
        factory = SimpleViewModelFactory {
            OnboardingViewModel(
                repository = ServiceLocator.repository,
                settings = ServiceLocator.settings
            )
        }
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
            text = "Выберите источник данных при первом запуске: бесплатные API " +
                "или личный ключ API-Football.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        when (state.selectedMode) {
            null -> ProviderChoice(
                checking = state.checking,
                onFreeClick = viewModel::chooseFreeApis,
                onPaidClick = viewModel::choosePaidKey
            )
            StartupMode.PAID_KEY -> PaidKeyForm(state = state, viewModel = viewModel)
            StartupMode.FREE_APIS -> Text(
                text = "Запускаем бесплатный режим…",
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchMuted
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = RMatchError
            )
        }
        Spacer(Modifier.height(16.dp))
        ResponsibleGamblingNote()
        Text(
            text = ApiConstants.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = RMatchMuted
        )
    }
}

@Composable
private fun ProviderChoice(
    checking: Boolean,
    onFreeClick: () -> Unit,
    onPaidClick: () -> Unit
) {
    Text(
        text = "Бесплатный режим использует TheSportsDB и OpenLigaDB без ключа. " +
            "Покрытие и глубина статистики могут быть ниже, чем у API-Football.",
        style = MaterialTheme.typography.bodyMedium,
        color = RMatchMuted
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onFreeClick,
        enabled = !checking,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (checking) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp))
        } else {
            Text("Продолжить с бесплатными API")
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onPaidClick,
        enabled = !checking,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Использовать ключ API-Football")
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Платный режим использует ${ApiConstants.PROVIDER_URL}. " +
            "Ключ хранится только на устройстве в защищённом хранилище.",
        style = MaterialTheme.typography.bodyMedium,
        color = RMatchMuted
    )
}

@Composable
private fun PaidKeyForm(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Text(
        text = "Зарегистрируйтесь на api-football.com или dashboard.api-football.com, " +
            "скопируйте API Key и вставьте его ниже.",
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
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = viewModel::backToChoice,
        enabled = !state.checking,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Назад к выбору")
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Ключ проверяется запросом к /status. Бесплатный тариф API-Football обычно " +
            "ограничен по суточной квоте, поэтому приложение кэширует ответы.",
        style = MaterialTheme.typography.bodyMedium,
        color = RMatchMuted
    )
}
