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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.tona.sandwave.engine.GameEngine
import com.tona.sandwave.R
import com.tona.sandwave.util.removeWhiteBackground
import com.tona.sandwave.util.resizeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import com.tona.sandwave.util.lerpColor
import com.tona.sandwave.util.rememberShieldBitmap
import kotlin.math.sin


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

    val shieldBitmap = rememberShieldBitmap()

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

    var colorTransitionStartTime by remember { mutableStateOf<Long?>(null) }

    // Nếu chargeProgress < max → reset transition
    if (chargeProgress < 2.5f) {
        colorTransitionStartTime = null
    }

    // Khi chargeProgress >= max → bắt đầu transition
    if (chargeProgress >= 2.5f && colorTransitionStartTime == null) {
        colorTransitionStartTime = System.currentTimeMillis()
    }

    // Tính t từ 0 → 1 dựa vào thời gian
    val colorTransitionDuration = 200L
    val t = colorTransitionStartTime?.let { start ->
        ((System.currentTimeMillis() - start).toFloat() / colorTransitionDuration).coerceIn(0f, 1f)
    } ?: 0f

    // Lưu vết trail
    val playerTrail = remember { mutableStateListOf<Pair<Offset, Long>>() }
    val trailDuration = 300L * (engine.obstacleSpeed / 5) // ms tồn tại của 1 vệt

    val blendedColor = lerpColor(
        from = Color.Yellow.copy(alpha = 0.5f),
        to = Color.Cyan.copy(alpha = 0.5f),
        t = t
    )

    LaunchedEffect (scale) {
        if (scale == 1f){
            engine.state.scale = scale
        } else if (scale < 1f){
            engine.state.scale = scale * 1.04f
        }
    }

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
            engine.isHolding && !engine.state.isPaused -> (chargeProgress + 0.016f).coerceAtMost(2.5f)
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
                        dstOffset = IntOffset(
                            (obj.x - (obj.size * 2.5f)).toInt(),   // 2.5f = 5/2
                            (obj.y - (obj.size * 2.5f)).toInt()
                        ),
                        dstSize = IntSize((obj.size * 5f).toInt(), (obj.size * 5f).toInt())
                    )
                }
            }

            // Shield object
            engine.state.shieldObjects.forEach { obj ->
                val center = Offset(obj.x, obj.y - obj.size * 0.5f) // đẩy lên cao hơn 1/2 size
                val radius = obj.size

                // Vẽ viền đen (stroke)
                drawCircle(
                    color = Color.Black,
                    radius = radius + 4f,
                    center = center,
                    style = Stroke(width = 6f)
                )

                // Vẽ hình shield ở trong
                drawImage(
                    image = shieldBitmap,
                    dstOffset = IntOffset(
                        (obj.x - obj.size).toInt(),
                        (obj.y - obj.size * 1.5f).toInt() // trừ thêm để đẩy cao lên
                    ),
                    dstSize = IntSize(obj.size.toInt() * 2, obj.size.toInt() * 2)
                )
            }

            // Item objects
            engine.state.itemObjects.forEach { obj ->
                // Vẽ vầng sáng vàng mờ
                drawCircle(
                    color = Color(0x66FFD700), // vàng mờ, alpha thấp
                    radius = obj.size * 2f,    // lớn hơn item để tạo halo
                    center = Offset(obj.x, obj.y)
                )

                // Vẽ item đỏ bên trong
                drawCircle(
                    color = Color.DarkGray,
                    radius = obj.size,
                    center = Offset(obj.x, obj.y)
                )
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

            // ---------- Ulti shockwave (clear screen) ----------
            if (engine.isUlti) {
                val elapsed = System.currentTimeMillis() - engine.ultiStart
                val progress = (elapsed.toFloat() / engine.ultiDuration).coerceIn(0f, 1f)

                val maxRadius = size.maxDimension * 1.2f
                val radius = maxRadius * progress
                val alpha = 1f - progress

                drawCircle(
                    color = Color.Cyan.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(engine.state.player.x, engine.state.player.y),
                    style = Stroke(width = 64f * (1f - progress))
                )

                // Khi ulti hết thì reset state
                if (progress >= 1f) {
                    engine.isUlti = false
                }
            }

            // ---------- Player ----------
            val playerPos = Offset(engine.state.player.x, engine.state.player.y)

            // Lưu lại vị trí player vào trail
            playerTrail.add(playerPos to now)

            // Xóa trail quá cũ
            playerTrail.removeAll { now - it.second > trailDuration }

            // Vẽ trail (đuôi tốc độ)
            playerTrail.forEachIndexed { index, (pos, time) ->
                val age = (now - time).toFloat() / trailDuration
                val alpha = (1f - age).coerceIn(0f, 1f)

                val shifted = pos.copy(x = pos.x - age * 50f) // lệch sang trái theo age

                drawCircle(
                    color = Color.DarkGray.copy(alpha = alpha * (1f - age * 1.75f)),
                    radius = 30f * (1f - age * 0.75f),
                    center = shifted
                )
            }

            if (chargeProgress > 0f && !engine.isUlti) {
                drawCircle(
                    color = blendedColor,
                    radius = playerRadius * chargeProgress * 2.2f,
                    center = Offset(engine.state.player.x, engine.state.player.y)
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 1f - 0.3f * chargeProgress),
                radius = playerRadius * (1f + 0.3f * chargeProgress),
                center = Offset(engine.state.player.x, engine.state.player.y)
            )

            // Vẽ hiệu ứng shield bao quanh player
            if (engine.isHoldingShield) {
                val remaining = (engine.shieldExpireTime - System.currentTimeMillis()).coerceAtLeast(0L)
                val progress = remaining / 5000f // 5 giây timeout
                val shieldColor = Color.Cyan.copy(alpha = 0.5f + 0.5f * progress)

                // Vòng ngoài nhấp nháy
                drawCircle(
                    color = shieldColor,
                    radius = playerRadius * 1.6f + (sin(System.currentTimeMillis() / 150.0) * 5).toFloat(),
                    center = Offset(engine.state.player.x, engine.state.player.y),
                    style = Stroke(width = 6f)
                )

                // Vòng glow mờ bên trong
                drawCircle(
                    color = shieldColor.copy(alpha = 0.3f),
                    radius = playerRadius * 2.0f,
                    center = Offset(engine.state.player.x, engine.state.player.y)
                )
            }

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
