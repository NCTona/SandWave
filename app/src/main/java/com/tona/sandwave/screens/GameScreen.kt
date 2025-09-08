package com.tona.sandwave.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tona.sandwave.R
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.graphic.GameGraphic
import com.tona.sandwave.thread.GameThread

@Composable
fun GameScreen(
    key: Int,
    onPause: () -> Unit,
    onGameOver: (Long) -> Unit,
    onPlayAgain: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Engine được reset lại mỗi khi key thay đổi
    var engine by remember(key) {
        mutableStateOf(GameEngine(1920f, 1080f, context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(engine) { // đảm bảo luôn dùng engine mới
                detectTapGestures(
                    onPress = {
                        engine.isHolding = true
                        engine.playerBoost()
                        tryAwaitRelease()
                        engine.isHolding = false
                    },
                    onTap = {
                        engine.playerJump()
                    }
                )
            }
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.sandworld),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Pause button
        if (!isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onPause,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Black.copy(0.8f))
                ) {
                    Text("Pause")
                }
            }

            // Score
            Text(
                text = "Score: ${engine.state.score}",
                fontSize = 20.sp,
                color = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

        }

        // Game graphic
        GameGraphic(
            engine = engine,
            modifier = Modifier.fillMaxSize()
        )
    }

    // Game thread
    DisposableEffect(engine, isPaused) {
        val gameThread = GameThread(
            engine = engine,
            onGameOver = {
                onGameOver(engine.state.score)  // Trả score ra ngoài
            },
            isPausedProvider = { isPaused },
            onReset = { onPlayAgain() }
        )

        gameThread.start()

        onDispose {
            gameThread.stopThread()
        }
    }

}
