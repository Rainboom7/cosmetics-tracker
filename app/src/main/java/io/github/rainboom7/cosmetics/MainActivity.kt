package io.github.rainboom7.cosmetics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.rainboom7.cosmetics.ui.navigation.CosmeticsNavHost
import io.github.rainboom7.cosmetics.ui.theme.CosmeticsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CosmeticsTheme {
                CosmeticsNavHost()
            }
        }
    }
}
