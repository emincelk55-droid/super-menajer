package com.supermenajer.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Screen
import com.supermenajer.game.ui.AppTheme
import com.supermenajer.game.ui.Bg
import com.supermenajer.game.ui.MainScreen
import com.supermenajer.game.ui.MatchScreen
import com.supermenajer.game.ui.NewGameScreen
import com.supermenajer.game.ui.SummaryScreen
import com.supermenajer.game.ui.TitleScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameRepo.init(filesDir)
        // Tam ekran oyun: sistem çubukları gizli, kaydırınca geçici olarak görünür
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize(), color = Bg) { AppRoot() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GameRepo.save(force = true)
        GameRepo.flush()
    }
}

@Composable
fun AppRoot() {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        when (GameRepo.screen) {
            Screen.SPLASH -> com.supermenajer.game.ui.SplashScreen()
            Screen.TITLE -> TitleScreen()
            Screen.NEW_GAME -> NewGameScreen()
            Screen.MAIN -> MainScreen()
            Screen.MATCH -> MatchScreen()
            Screen.SUMMARY -> SummaryScreen()
        }
        GameRepo.busy?.let { com.supermenajer.game.ui.BusyOverlay(it) }
    }
}
