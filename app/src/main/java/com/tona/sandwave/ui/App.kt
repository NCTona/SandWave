package com.tona.sandwave.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tona.sandwave.screens.*

enum class Screen { MENU, GAME, PAUSE, GAMEOVER }

@Composable
fun App() {
    val currentScreen = remember { mutableStateOf(Screen.MENU) }

    when (currentScreen.value) {
        Screen.MENU -> MenuScreen(onPlay = { currentScreen.value = Screen.GAME })
        Screen.GAME -> GameScreen(
            onPause = { currentScreen.value = Screen.PAUSE },
            onGameOver = { currentScreen.value = Screen.GAMEOVER }
        )
        Screen.PAUSE -> PauseScreen(
            onContinue = { currentScreen.value = Screen.GAME },
            onMenu = { currentScreen.value = Screen.MENU }
        )
        Screen.GAMEOVER -> GameOverScreen(
            onPlayAgain = { currentScreen.value = Screen.GAME },
            onMenu = { currentScreen.value = Screen.MENU }
        )
    }
}
