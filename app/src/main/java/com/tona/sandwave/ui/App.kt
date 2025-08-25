package com.tona.sandwave.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.screens.*

@Composable
fun App() {
    val showMenu = remember { mutableStateOf(true) }
    val showPause = remember { mutableStateOf(false) }
    val showGameOver = remember { mutableStateOf(false) }

    var reset by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        // GameScreen luôn tồn tại nhưng reset khi cần
        GameScreen(
            key = reset,
            onPause = { showPause.value = true },
            onGameOver = { showGameOver.value = true },
            onPlayAgain = { showGameOver.value = false },
            isPaused = showPause.value || showMenu.value || showGameOver.value
        )

        // Overlay MENU
        if (showMenu.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                MenuScreen(onPlay = {
                    showMenu.value = false
                })
            }
        }

        // Overlay PAUSE
        if (showPause.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                PauseScreen(
                    onContinue = { showPause.value = false },
                    onMenu = {
                        showPause.value = false
                        showMenu.value = true
                        reset += 1
                    }
                )
            }
        }

        // Overlay GAMEOVER
        if (showGameOver.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                GameOverScreen(
                    onPlayAgain = {
                        reset += 1
                        showGameOver.value = false
                        reset += 1
                    },
                    onMenu = {
                        showGameOver.value = false
                        showMenu.value = true
                        reset += 1
                    }
                )
            }
        }
    }
}
