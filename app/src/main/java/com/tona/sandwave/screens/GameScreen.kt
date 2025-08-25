package com.tona.sandwave.screens

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tona.sandwave.engine.GameEngine
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun GameScreen(
    onPause: () -> Unit,
    onGameOver: () -> Unit
) {
    var engine by remember { mutableStateOf<GameEngine?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    engine?.playerJump()
                })
            }
    ) {
        // Nút Pause góc trên phải
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
            if (engine == null) {
                engine = GameEngine(size.width, size.height)
            }

            engine?.let { eng ->
                val amplitude = 100f
                val length = 300f
                val radius = 30f

                // Obstacles: đáy chạm sóng
                eng.state.obstacles.forEach { obs ->
                    val obsY = size.height / 2 + amplitude * sin((obs.x + eng.waveOffset)/length)
                    val topY = obsY - obs.height
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(obs.x, topY),
                        size = Size(obs.width, obs.height + 10f)
                    )
                }

                // Sóng cát
                drawPath(
                    path = Path().apply {
                        moveTo(0f, size.height) // góc trái dưới

                        // vẽ sóng đến gần cuối màn hình
                        for (x in 0..size.width.toInt() step 10) {
                            val y = size.height / 2 + amplitude * sin((x + eng.waveOffset)/length)
                            lineTo(x.toFloat(), y)
                        }

                        // ép thêm 1 điểm chính xác tại mép phải
                        val lastY = size.height / 2 + amplitude * sin((size.width + eng.waveOffset)/length)
                        lineTo(size.width, lastY)

                        // đóng path về đáy phải
                        lineTo(size.width, size.height)
                        close()
                    },
                    color = Color(0xFFFFD700)
                )



                // Player lướt trên sóng
                drawCircle(
                    color = Color.Red,
                    radius = radius,
                    center = Offset(eng.state.player.x, eng.state.player.y)
                )
            }
        }
    }

    // Vòng lặp update game
    LaunchedEffect(engine) {
        while (true) {
            engine?.update()
            if (engine?.isGameOver == true) {
                onGameOver()
                break
            }
            delay(8) // ~60FPS
        }
    }
}
