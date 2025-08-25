package com.tona.sandwave.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.tona.sandwave.engine.GameEngine
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    key: Int,
    onPause: () -> Unit,
    onGameOver: () -> Unit,
    onPlayAgain: () -> Unit,
    isPaused: Boolean,
) {
    var engine by remember { mutableStateOf<GameEngine?>(null) }

    // Biến lưu scale hiện tại và thời điểm player nhảy cao
    var scale by remember { mutableStateOf(1f) }
    var lastHighJumpTime by remember { mutableStateOf(0L) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFACE4EF))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        engine?.isHolding = true
                        engine?.playerFly()
                        tryAwaitRelease()
                        engine?.isHolding = false
                    },
                    onTap = { engine?.playerJump() }
                )
            }
    ) {
        // Nút Pause
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = { onPause() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text("Pause")
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (engine == null) engine = GameEngine(size.width, size.height)

            engine?.let { eng ->
                val playerRadius = 30f
                val paddingTop = size.height * 0.2f // khoảng cách tham chiếu
                val scaleDuration = 5000L // giữ scale tối đa trong 5s
                val scaleSpeed = 0.0005f   // tốc độ tăng dần về 1 mỗi frame

                val playerTop = eng.state.player.y - playerRadius + 50f
                val targetScale = if (playerTop < paddingTop) {
                    (playerTop + 1000f) / (paddingTop + 1000f)
                } else 1f

                // xử lý khi player nhảy cao
                if (targetScale < 1f) {
                    lastHighJumpTime = System.currentTimeMillis()
                    if (scale > targetScale) {
                        // scale hiện tại lớn hơn targetScale -> giảm xuống targetScale
                        scale = targetScale
                    }
                    // nếu targetScale > scale hiện tại -> giữ scale hiện tại
                }

                // nếu đã hết 5s kể từ nhảy cao -> tăng dần về 1
                if (System.currentTimeMillis() - lastHighJumpTime > scaleDuration) {
                    if (scale < 1f) {
                        scale += scaleSpeed
                        if (scale > 1f) scale = 1f
                    }
                }

                // áp dụng scale đồng nhất quanh trung tâm màn hình
                with(drawContext.canvas) {
                    save()
                    val pivotX = size.width / 2f
                    val pivotY = size.height / 2f
                    scale(scale, scale, pivotX, pivotY)

                    // vẽ obstacles
                    eng.state.obstacles.forEach { obs ->
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height + 20f)
                        )
                    }

                    // vẽ sóng
                    drawPath(
                        path = Path().apply {
                            val extendedWidth = size.width / scale + 200f
                            val extendedHeight = size.height / scale + 200f

                            moveTo(-400f, extendedHeight)
                            var sx = -400f
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
                        color = Color(0xFFF1B42E)
                    )

                    // vẽ player
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

    // Vòng lặp update game
    LaunchedEffect(engine, isPaused, key) {
        while (true) {
            if (!isPaused) {
                engine?.update()
                if (engine?.state?.isGameOver  == true) {
                    onGameOver()
                    engine?.state?.isGameOver  = false
                    break
                }
            }
            delay(8) // ~60FPS
        }
    }

    // Reset game
    LaunchedEffect(key) {
        engine?.reset()
        delay(20)
        onPlayAgain()
    }
}
