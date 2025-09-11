package com.tona.sandwave.graphic

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.scale
import com.tona.sandwave.engine.GameEngine

@Composable
fun GameGraphic(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    // Nội bộ quản lý scale và thời gian nhảy cao
    var scale by remember { mutableStateOf(1f) }
    var lastHighJumpTime by remember { mutableStateOf(0L) }

    Canvas(modifier = modifier) {
        val playerRadius = 30f
        val paddingTop = size.height * 0.25f
        val scaleDuration = 5000L
        val scaleSpeed = 0.0005f

        val playerTop = engine.state.player.y - playerRadius
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
            scale(scale, scale, engine.state.player.x, pivotY)

            // Obstacles
            engine.state.obstacles.forEach { obs ->
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(obs.x, obs.y),
                    size = Size(obs.width, obs.height + 100f)
                )
            }

            // Wave
            drawPath(
                path = Path().apply {
                    val extendedWidth = size.width / scale + 600f
                    val extendedHeight = size.height / scale + 600f

                    moveTo(-5000f, extendedHeight)
                    var sx = -5000f
                    val step = 6f
                    while (sx <= extendedWidth) {
                        val worldX = sx + engine.waveOffset
                        val y = engine.getWaveHeightAt(worldX)
                        lineTo(sx, y)
                        sx += step
                    }
                    lineTo(extendedWidth, extendedHeight)
                    close()
                },
                color = Color(0xFFDCA32A)
            )

            // Player
            drawCircle(
                color = Color.Black,
                radius = playerRadius,
                center = Offset(engine.state.player.x, engine.state.player.y)
            )

            restore()
        }
    }
}
