package com.tona.sandwave.ui

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tona.sandwave.screens.*

@Composable
fun App() {
    val showMenu = remember { mutableStateOf(true) }
    val showPause = remember { mutableStateOf(false) }
    val showGameOver = remember { mutableStateOf(false) }

    var reset by remember { mutableStateOf(0) }

    // Animate blur radius (mượt)
    val blurRadius by animateDpAsState(
        targetValue = if (showPause.value || showMenu.value || showGameOver.value) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500) // 0.5 giây
    )

    // Animate background alpha (mượt)
    val overlayAlpha by animateFloatAsState(
        targetValue = if (showPause.value || showMenu.value || showGameOver.value) 0.5f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(Modifier.fillMaxSize()) {
        // GameScreen luôn tồn tại nhưng reset khi cần
        GameScreen(
            key = reset,
            onPause = { showPause.value = true },
            onGameOver = { showGameOver.value = true },
            onPlayAgain = { showGameOver.value = false },
            isPaused = showPause.value || showMenu.value || showGameOver.value,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius) // blur có animation
        )

        // Overlay mượt (dùng chung cho cả Menu / Pause / GameOver)
        if (showMenu.value || showPause.value || showGameOver.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showMenu.value -> MenuScreen(onPlay = {
                        showMenu.value = false
                        showPause.value = false
                    })
                    showPause.value -> PauseScreen(
                        onContinue = { showPause.value = false },
                        onMenu = {
                            showPause.value = false
                            showMenu.value = true
                            reset += 1
                        }
                    )
                    showGameOver.value -> GameOverScreen(
                        onPlayAgain = {
                            reset += 1
                            showPause.value = false
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
}
