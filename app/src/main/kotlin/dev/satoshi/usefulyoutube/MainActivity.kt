package dev.satoshi.usefulyoutube

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.navigation.UsefulYouTubeNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The app is always dark, so the system bars always use light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            UsefulYouTubeTheme {
                UsefulYouTubeNavHost(recommendationsEnabled = state.recommendationsEnabled)
            }
        }
    }
}
