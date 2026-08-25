package com.rmatch.football

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rmatch.football.navigation.RMatchRoot
import com.rmatch.football.ui.theme.RMatchBackground
import com.rmatch.football.ui.theme.RMatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RMatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RMatchBackground
                ) {
                    RMatchRoot()
                }
            }
        }
    }
}
