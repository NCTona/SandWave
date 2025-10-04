package com.tona.sandwave.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
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
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    engine: GameEngine,
    onPause: () -> Unit,
    onGameOver: (Long) -> Unit,
    onWinGame: (Long) -> Unit,
    onPlayAgain: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {


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

    // ---------------- BACKGROUND ----------------
    val background = ImageBitmap.imageResource(id = R.drawable.sandwave_background)
    val backgroundWidth = background.width.toFloat()

    var offsetX by remember { mutableStateOf(0f) }

    // Cập nhật offset liên tục để tạo hiệu ứng cuộn
    LaunchedEffect(isPaused) {
        while (true) {
            if (!isPaused) {
                offsetX -= engine.obstacleSpeed   // tốc độ chạy
                if (offsetX <= -backgroundWidth) {
                    offsetX = 0f
                }
            }
            delay(16) // ~60fps
        }
    }

    var isMusic by remember { mutableStateOf(true) }
    LaunchedEffect(isMusic) {
        if (isMusic) {
            engine.musicPlayer.play()
        } else {
            engine.musicPlayer.pause()
        }
    }

    var isSound by remember { mutableStateOf(true) }
    LaunchedEffect(isSound) {
        if (isSound) {
            engine.sound.enableSound()
        } else {
            engine.sound.disableSound()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(engine, isPaused) {
                detectTapGestures(
                    onPress = {
                        if (!isPaused && engine.state.player.isJumping) {
                            engine.isHolding = true
                            val startTime = System.nanoTime()
                            tryAwaitRelease()
                            val holdTime = (System.nanoTime() - startTime) / 1_000_000_000f
                            engine.playerUlti(holdTime)
                            engine.isHolding = false
                        } else if (!isPaused && !engine.state.player.isJumping) {
                            engine.isHolding = true
                            tryAwaitRelease()
                            engine.playerJump()
                            engine.isHolding = false
                        }
                    },
                    onTap = {
                        if (!isPaused && !engine.isHolding) {
                            engine.playerJump()
                        }
                    }
                )
            }
    ) {
        // Vẽ background cuộn
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(background, topLeft = Offset(offsetX, 0f))
            drawImage(background, topLeft = Offset(offsetX + backgroundWidth, 0f))
        }

        // Pause button + Score
        if (!isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onPause,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
                ) {
                    Text("Pause")
                }
            }

            Column(
                modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
            ){
                Text(
                    text = "Score: ${engine.state.score}",
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(4.dp)
                )
                Text(
                    text = "Coin: ${engine.state.coin}",
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(4.dp)
                )
                Text(
                    text = "Distance: ${engine.state.distance.toLong()}",
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
        }

        // Game graphic
        GameGraphic(
            engine = engine,
            modifier = Modifier.fillMaxSize()
        )

        // Nút góc dưới trái
        IconButton(
            onClick = { isMusic = !isMusic }, // toggle
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isMusic) R.drawable.music
                    else R.drawable.not_music
                ),
                contentDescription = "Music Toggle",
                tint = Color.Black
            )
        }

        // Nút góc dưới phải
        IconButton(
            onClick = {
                isSound = !isSound
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isSound) R.drawable.baseline_headset_24
                    else R.drawable.not_baseline_headset_24
                ),
                contentDescription = "Music Toggle",
                tint = Color.Black
            )
        }

    }

    // ---------------- GAME THREAD ----------------
    DisposableEffect(engine, isPaused) {
        val gameThread = GameThread(
            engine = engine,
            onGameOver = { onGameOver(engine.state.score) },
            onWinGame = { onWinGame(engine.state.score) },
            isPausedProvider = { isPaused },
            onReset = { onPlayAgain() }
        )

        gameThread.start()
        onDispose { gameThread.stopThread() }
    }
}
