package dev.satoshi.usefulyoutube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.navigation.UsefulYouTubeNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsefulYouTubeTheme {
                UsefulYouTubeNavHost()
            }
        }
    }
}
