package com.tona.sandwave.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tona.sandwave.R
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.thread.GameThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun GameScreen(
    key: Int,
    onPause: () -> Unit,
    onGameOver: () -> Unit,
    onPlayAgain: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {

    var previousKey by remember { mutableStateOf<Int?>(null) }
    var previousEngine by remember { mutableStateOf<GameEngine?>(null) }
    var previousIsPaused by remember { mutableStateOf<Boolean?>(null) }

    var engine by remember { mutableStateOf<GameEngine?>(null) }
    val context = LocalContext.current

    // Biến lưu scale hiện tại và thời điểm player nhảy cao
    var scale by remember { mutableStateOf(1f) }
    var lastHighJumpTime by remember { mutableStateOf(0L) }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        engine?.isHolding = true
                        engine?.playerBoost()
                        tryAwaitRelease()
                        engine?.isHolding = false
                    },
                    onTap = {
                        engine?.playerJump()
                    }
                )
            }
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.sandworld), // đặt ảnh background ở res/drawable
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // phủ toàn màn hình
        )

        if(!isPaused){
            // Nút Pause
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
                    Text("Pause", modifier = Modifier.background(Color.Black.copy(alpha = 0f)))
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (engine == null) engine = GameEngine(size.width, size.height, context)

            engine?.let { eng ->
                val playerRadius = 30f
                val paddingTop = size.height * 0.25f
                val scaleDuration = 5000L
                val scaleSpeed = 0.0005f

                val playerTop = eng.state.player.y - playerRadius
                val targetScale = if (playerTop < paddingTop) {
                    (playerTop + 1000f) / (paddingTop + 1000f)
                } else 1f

                if (targetScale < 1f) {
                    lastHighJumpTime = System.currentTimeMillis()
                    if (scale > targetScale) scale = targetScale
                }

                if (System.currentTimeMillis() - lastHighJumpTime > scaleDuration) {
                    if (scale < 1f) {
                        scale += scaleSpeed
                        if (scale > 1f) scale = 1f
                    }
                }

                with(drawContext.canvas) {
                    save()
                    val pivotY = size.height / 2f
                    scale(scale, scale,eng.state.player.x , pivotY)

                    // Vẽ obstacles
                    eng.state.obstacles.forEach { obs ->
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height + 20f)
                        )
                    }

                    // Vẽ sóng
                    drawPath(
                        path = Path().apply {
                            val extendedWidth = size.width / scale + 600f
                            val extendedHeight = size.height / scale + 600f

                            moveTo(-1000f, extendedHeight)
                            var sx = -1000f
                            val step = 6f
                            while (sx <= extendedWidth) {
                                val worldX = sx + eng.waveOffset
                                val y = eng.getWaveHeightAt(worldX)
                                lineTo(sx, y)
                                sx += step
                            }
                            lineTo(extendedWidth, extendedHeight)
                            close()
                        },
                        color = Color(0xFFDCA32A)
                    )

                    // Vẽ player
                    drawCircle(
                        color = Color.Black,
                        radius = playerRadius,
                        center = Offset(eng.state.player.x, eng.state.player.y)
                    )

                    restore()
                }
            }
        }
    }

    DisposableEffect(engine, isPaused, key) {
        val gameThread = engine?.let {
            GameThread(
                engine = it,
                onGameOver = onGameOver,
                isPausedProvider = { isPaused },
                onReset = { onPlayAgain() }
            )
        }

        // So sánh key
        if (previousKey != null && previousKey != key) {
            gameThread?.requestReset()
        }

        // So sánh engine
        if (previousEngine != null && previousEngine != engine) {
            // Nếu muốn: restart game thread
        }

        // So sánh isPaused
        if (previousIsPaused != null && previousIsPaused != isPaused) {
            // Chỉ cần thread đọc isPausedProvider thôi, không reset lại
        }

        previousKey = key
        previousEngine = engine
        previousIsPaused = isPaused

        gameThread?.start()

        onDispose {
            gameThread?.stopThread()
        }
    }

}
