package com.tona.sandwave.ui

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.io.GameIO
import com.tona.sandwave.screens.*

@Composable
fun App(
    engine: GameEngine,
        eventAffect: MutableState<Boolean>,
) {

    val context = LocalContext.current

    val showMenu = remember { mutableStateOf(true) }
    val showPause = remember { mutableStateOf(false) }
    val showGameOver = remember { mutableStateOf(false) }

    var score by remember { mutableStateOf(0L) }

    val isPaused by remember {
        derivedStateOf { engine.state.isPaused }
    }

    // Animate blur radius (mượt)
    val blurRadius by animateDpAsState(
        targetValue = if (isPaused) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500) // 0.5 giây
    )

    // Animate background alpha (mượt)
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPaused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(showMenu.value, showPause.value, showGameOver.value) {
        engine.state.isPaused = showMenu.value || showPause.value || showGameOver.value
    }

    LaunchedEffect(eventAffect) {
        if (eventAffect.value && !showMenu.value && !showGameOver.value) {
            showPause.value = true
            eventAffect.value = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        // GameScreen luôn tồn tại nhưng reset khi cần
        GameScreen(
            onPause = { showPause.value = true },
            onGameOver = {sc ->
                score = sc
                GameIO.saveHighScore(context, score)
                showGameOver.value = true
            },
            engine = engine,
            onPlayAgain = { showGameOver.value = false },
            isPaused = isPaused,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius) // blur có animation
        )

        // Overlay mượt (dùng chung cho cả Menu / Pause / GameOver)
        if (isPaused) {
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
                            engine.state.isReset = true
                        }
                    )
                    showGameOver.value -> GameOverScreen(
                        onPlayAgain = {
//                            engine.state.isReset = true
                            showPause.value = false
                            showGameOver.value = false
                            engine.state.isReset = true
                        },
                        onMenu = {
                            showGameOver.value = false
                            showMenu.value = true
                            engine.state.isReset = true
                        },
                        score = score,
                        highScore = GameIO.getHighScore(context)
                    )
                }
            }
        }
    }
}
