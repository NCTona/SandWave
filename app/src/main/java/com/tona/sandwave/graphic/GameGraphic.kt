package com.tona.sandwave.graphic

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.R
import com.tona.sandwave.util.removeWhiteBackground
import com.tona.sandwave.util.resizeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Hiệu ứng shockwave/vòng tròn
data class ReleaseEffect(
    val isPlayer: Boolean = false,
    val startTime: Long,
    val center: Offset,
    val duration: Long = 600L
)

@Composable
fun GameGraphic(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Meteor bitmap chạy trên background thread, Compose sẽ tự động recomposition khi load xong
    var meteorBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        // Chạy trên thread Default
        val bitmap = withContext(Dispatchers.Default) {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.meteor)
            val processed = resizeBitmap(removeWhiteBackground(raw), 60, 60)
            processed.asImageBitmap()
        }
        meteorBitmap = bitmap
    }

    var scale by remember { mutableStateOf(1f) }
    var lastHighJumpTime by remember { mutableStateOf(0L) }
    var chargeProgress by remember { mutableStateOf(0f) }
    val releaseEffects = remember { mutableStateListOf<ReleaseEffect>() }
    var lastHolding by remember { mutableStateOf(false) }

    Canvas(modifier = modifier) {
        val playerRadius = 30f
        val paddingTop = size.height * 0.25f
        val scaleDuration = 5000L
        val scaleSpeed = 0.0005f

        // ---------- Hiệu ứng zoom khi nhảy cao ----------
        val playerTop = engine.state.player.y - playerRadius
        val targetScale = if (playerTop < paddingTop) (playerTop + 1000f) / (paddingTop + 1000f) else 1f
        if (targetScale < 1f) {
            lastHighJumpTime = System.currentTimeMillis()
            if (scale > targetScale) scale = targetScale
        }
        if (System.currentTimeMillis() - lastHighJumpTime > scaleDuration) {
            scale = (scale + scaleSpeed).coerceAtMost(1f)
        }

        // ---------- Reset ----------
        if (engine.state.isReset) {
            chargeProgress = 0f
            releaseEffects.clear()
        }

        // ---------- Tụ lực ----------
        chargeProgress = when {
            engine.isHolding && !engine.state.isPaused -> (chargeProgress + 0.016f).coerceAtMost(2f)
            else -> (chargeProgress - 0.02f).coerceAtLeast(0f)
        }

        // Phát hiện nhả tay → shockwave tại player
        if (lastHolding && !engine.isHolding && chargeProgress > 0.2f) {
            releaseEffects.add(
                ReleaseEffect(
                    isPlayer = true,
                    startTime = System.currentTimeMillis(),
                    center = Offset(engine.state.player.x, engine.state.player.y)
                )
            )
        }
        lastHolding = engine.isHolding

        // Poll shockwave mới từ engine
        while (true) {
            val p = engine.pendingShockwaves.poll() ?: break
            releaseEffects.add(
                ReleaseEffect(
                    isPlayer = false,
                    startTime = System.currentTimeMillis(),
                    center = Offset(p.first, p.second)
                )
            )
        }

        with(drawContext.canvas) {
            save()
            val pivotY = size.height / 2f
            scale(scale, scale, engine.state.player.x, pivotY)

            // Obstacles (copy để tránh ConcurrentModification)
            val obstaclesCopy = engine.state.obstacles.toList()
            obstaclesCopy.forEach { obs ->
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
                color = Color(0xFFC79F00)
            )

            // Falling objects
            // Khi vẽ trong Canvas, kiểm tra null
            meteorBitmap?.let { bitmap ->
                val fallingCopy = engine.state.fallingObjects.toList()
                fallingCopy.forEach { obj ->
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset((obj.x - obj.size).toInt(), (obj.y - obj.size).toInt()),
                        dstSize = IntSize((obj.size * 5.0f).toInt(), (obj.size * 5.0f).toInt())
                    )
                }
            }



            // ---------- Hiệu ứng shockwave ----------
            val now = System.currentTimeMillis()
            val iter = releaseEffects.iterator()
            while (iter.hasNext()) {
                val effect = iter.next()
                val progress = (now - effect.startTime) / effect.duration.toFloat()
                if (progress >= 1f) iter.remove()
                else {
                    val radius = playerRadius * (1f + 4f * progress)
                    val alpha = 1f - progress
                    drawCircle(
                        color = Color(0x66FFD700).copy(alpha = alpha),
                        radius = radius,
                        center = if (effect.isPlayer) Offset(engine.state.player.x, engine.state.player.y) else effect.center
                    )
                }
            }

            // ---------- Player ----------
            if (chargeProgress > 0f) {
                drawCircle(
                    color = Color(0x66FFD700),
                    radius = playerRadius * chargeProgress * 2.2f,
                    center = Offset(engine.state.player.x, engine.state.player.y)
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 1f - 0.3f * chargeProgress),
                radius = playerRadius * (1f + 0.3f * chargeProgress),
                center = Offset(engine.state.player.x, engine.state.player.y)
            )

            // Projectiles (copy danh sách)
            engine.state.projectiles.toList().forEach { proj ->
                drawCircle(
                    color = Color(0xBBFFD700),
                    radius = proj.radius,
                    center = Offset(proj.x, proj.y)
                )
            }

            restore()
        }
    }
}

