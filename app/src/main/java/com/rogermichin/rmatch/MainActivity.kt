package com.rogermichin.rmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rogermichin.rmatch.ui.MainViewModel
import com.rogermichin.rmatch.ui.MainViewModelFactory
import com.rogermichin.rmatch.ui.RMatchApp
import com.rogermichin.rmatch.ui.theme.RMatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as RMatchApplication
            RMatchRoot(app.container)
        }
    }
}

@Composable
private fun RMatchRoot(container: AppContainer) {
    val factory = remember(container) { MainViewModelFactory(container.repository) }
    val viewModel: MainViewModel = viewModel(factory = factory)
    RMatchTheme {
        RMatchApp(viewModel = viewModel)
    }
}
