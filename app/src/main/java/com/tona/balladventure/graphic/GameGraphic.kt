package com.tona.balladventure.graphic

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.tona.balladventure.engine.GameEngine
import com.tona.balladventure.util.removeWhiteBackground
import com.tona.balladventure.util.resizeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.util.lerp
import com.tona.balladventure.type.Types
import com.tona.balladventure.util.cropNonTransparentArea
import com.tona.balladventure.util.lerpColor
import com.tona.balladventure.util.rememberShieldBitmap
import com.tona.balladventure.R
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


// Hiệu ứng shockwave/vòng tròn
data class ReleaseEffect(
    val isPlayer: Boolean = false,
    val startTime: Long,
    val center: Offset,
    val duration: Long = 600L
)

@SuppressLint("RestrictedApi")
@Composable
fun GameGraphic(
    engine: GameEngine,
    modifier: Modifier = Modifier,
    onLoaded: () -> Unit
) {
    val context = LocalContext.current

    // Meteor bitmap chạy trên background thread, Compose sẽ tự động recomposition khi load xong
    var meteorBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Shield bitmap
    val shieldBitmap = rememberShieldBitmap()

    // Obstacle bitmap
    var obstacleBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var obstacleBitmapFlip by remember { mutableStateOf<ImageBitmap?>(null) }


    LaunchedEffect(Unit) {
        // Chạy tất cả xử lý bitmap trên thread Default
        val (meteor, obstacle, obstacleFlip) = withContext(Dispatchers.Default) {
            // Meteor
            val rawMeteor = BitmapFactory.decodeResource(context.resources, R.drawable.meteor)
            val processedMeteor = resizeBitmap(removeWhiteBackground(rawMeteor), 60, 60)

            // Obstacle
            val rawCactus = BitmapFactory.decodeResource(context.resources, R.drawable.cactus)
            val croppedCactus = cropNonTransparentArea(rawCactus)

            val rawCactusFlip = BitmapFactory.decodeResource(context.resources, R.drawable.cactus_flip)
            val croppedCactusFlip = cropNonTransparentArea(rawCactusFlip)

            Triple(processedMeteor.asImageBitmap(), croppedCactus.asImageBitmap(), croppedCactusFlip.asImageBitmap())
        }

        // Gán vào state (lưu ý phải trên main thread, Compose sẽ tự động đảm bảo)
        meteorBitmap = meteor
        obstacleBitmap = obstacle
        obstacleBitmapFlip = obstacleFlip

        // Gọi callback
        onLoaded()
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
    val colorTransitionDuration = 250L
    val t = colorTransitionStartTime?.let { start ->
        ((System.currentTimeMillis() - start).toFloat() / colorTransitionDuration).coerceIn(0f, 1f)
    } ?: 0f

    // Lưu vết trail
    val playerTrail = remember { mutableStateListOf<Pair<Offset, Long>>() }
    val trailDuration = 100L * (engine.obstacleSpeed / 5) // ms tồn tại của 1 vệt

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
        var paddingTop = size.height * 0.25f
        val basePaddingTop = size.height * 0.25f
        val playerRadius = 30f
        val scaleDuration = 5000L
        val scaleSpeed = 0.0005f

        // giới hạn playerTop không vượt quá -1000
        val rawPlayerTop = engine.state.player.y - playerRadius
        val playerTop = rawPlayerTop.coerceAtLeast(-1000f)

        // tính targetScale
        val targetScale = if (playerTop < paddingTop) (playerTop + 1200f) / (paddingTop + 1200f) else 1f

        if (targetScale < 1f && targetScale > 0.3f) {
            lastHighJumpTime = System.currentTimeMillis()
            if (scale > targetScale) {
                scale = targetScale

                // tăng tốc độ giảm của paddingTop dựa theo scale
                val fastFactor = 5f // càng >1 giảm càng nhanh
                paddingTop = basePaddingTop * (1f - (1f - scale) * fastFactor)
                paddingTop = paddingTop.coerceAtLeast(-1000f)
            }
        }

        // hồi phục scale và paddingTop
        if (System.currentTimeMillis() - lastHighJumpTime > scaleDuration) {
            scale = (scale + scaleSpeed).coerceAtMost(1f)

            // paddingTop đồng bộ với scale
            val fastFactor = 1.5f
            paddingTop = basePaddingTop * (1f - (1f - scale) * fastFactor)
            paddingTop = paddingTop.coerceAtMost(basePaddingTop) // không vượt quá base
        }

        // reset khi cần
        if (engine.state.isReset) {
            chargeProgress = 0f
            releaseEffects.clear()
            playerTrail.clear()
        }

        // charge logic (UI-side progress)
        chargeProgress = when {
            engine.isHolding && !engine.state.isPaused -> (chargeProgress + 0.022f).coerceAtMost(2.5f)
            else -> chargeProgress // Không reset về 0 ngay lập tức
        }

        // phát hiện nhả tay -> thêm ReleaseEffect (player)
        if (lastHolding && !engine.isHolding && chargeProgress > 0.2f) {
            releaseEffects.add(
                ReleaseEffect(
                    isPlayer = true,
                    startTime = System.currentTimeMillis(),
                    center = Offset(engine.state.player.x, engine.state.player.y),
                    duration = 600L // thời gian toả dần
                )
            )
            chargeProgress = 0f // reset chargeProgress sau khi tạo ReleaseEffect
        }
        lastHolding = engine.isHolding


        // lấy shockwaves từ engine (nếu có)
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

            // obstacle
            val obstaclesSnapshot = engine.state.obstacles.toList()
            obstaclesSnapshot.forEach { obs ->

                if (obs.isTop) {
                    obstacleBitmapFlip?.let {
                        drawImage(
                            image = it, // bitmap PNG của bạn
                            dstOffset = IntOffset(obs.x.toInt(), obs.y.toInt() - 50), // vị trí góc trên trái
                            dstSize = IntSize(obs.width.toInt() , obs.height.toInt()) // kích thước obs
                        )
                    }
                } else {
                    obstacleBitmap?.let {
                        drawImage(
                            image = it, // bitmap PNG của bạn
                            dstOffset = IntOffset(obs.x.toInt(), obs.y.toInt()), // vị trí góc trên trái
                            dstSize = IntSize(obs.width.toInt() , obs.height.toInt() + 50) // kích thước obs
                        )
                    }
                }
            }

            // enemy
            val enemySnapshot = engine.state.enemies.toList()
            enemySnapshot.forEach { enm ->
                val center = Offset(enm.x + enm.width / 2, enm.y + enm.height / 2)
                val baseRadius = enm.size

                // Glow mờ bên ngoài
                drawCircle(
                    color = Color.Red.copy(alpha = 0.25f),
                    center = center,
                    radius = baseRadius * 1.8f
                )

                // Viền halo mỏng
                drawCircle(
                    color = Color.Red.copy(alpha = 0.6f),
                    center = center,
                    radius = baseRadius * 1.3f,
                    style = Stroke(width = 4f)
                )

                // Core enemy
                drawCircle(
                    color = Color.Red,
                    center = center,
                    radius = baseRadius
                )
            }


            if (engine.state.gameType == Types.JUMP){
                // warning zone
                engine.waveData.waveGaps.forEach { (start, end) ->
                    val width = end - start
                    val innerStart = start + width * 0.3f
                    val innerEnd = end - width * 0.3f

                    // Tâm hình tròn đặt dưới màn hình
                    val centerX = (innerStart + innerEnd) / 2f - engine.waveOffset
                    val centerY = engine.canvasHeight + width

                    // Bán kính = khoảng cách từ tâm đến biên trap
                    val radius = sqrt(
                        (centerX - (innerStart - engine.waveOffset)).pow(2) +
                                (centerY - engine.getWaveHeightAt(innerStart)).pow(2)
                    )

                    val arcTopLeft = Offset(centerX - radius, centerY - radius)
                    val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

                    // ======= Tạo hiệu ứng glow tan ra =======
                    val duration = 500  // thời gian 1 chu kỳ (ms)
                    val t = (System.currentTimeMillis() % duration).toFloat() / duration  // t ∈ [0,1]

                    // Viền to ra theo t
                    val strokeWidth = lerp(0f, 100f, t)

                    // Alpha giảm dần theo t
                    val alpha = lerp(1f, 0f, t)

                    // Vẽ glow nửa cung tan ra
                    drawArc(
                        color = Color(0xFFFF3333).copy(alpha = alpha),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Vẽ nửa cung cố định dưới để luôn giữ hình dạng trap
                    drawArc(
                        color = Color(0xFFFF3333),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 10f)
                    )
                }
            }

            // wave
            if (engine.state.gameType != Types.FLY) {
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
                    color = if (engine.state.gameType == Types.JUMP) Color(0xFFC79F00) else Color(
                        0xFF0F5952
                    )
                )
            }

            // wave top
            if (engine.state.gameType == Types.SWITCH) {
                drawPath(
                    path = Path().apply {
                        val extendedWidth = size.width / scale + 600f
                        val extendedHeight = size.height / scale + 600f
                        moveTo(-5000f, -extendedHeight)
                        var sx = -5000f
                        val step = 6f
                        while (sx <= extendedWidth) {
                            val worldX = sx + engine.waveOffset
                            val y = engine.getWaveTopHeightAt(worldX)
                            lineTo(sx, y)
                            sx += step
                        }
                        lineTo(extendedWidth, -extendedHeight)
                        close()
                    },
                    color = Color(
                        0xFF0F5952
                    )
                )
            }

            // falling objects (snapshot)
            meteorBitmap?.let { bitmap ->
                val fallingSnapshot = engine.state.fallingObjects.toList()
                fallingSnapshot.forEach { obj ->
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(
                            (obj.x - (obj.size * 2.5f)).toInt(),
                            (obj.y - (obj.size * 2.5f)).toInt()
                        ),
                        dstSize = IntSize((obj.size * 5f).toInt(), (obj.size * 5f).toInt())
                    )
                }
            }

            // shield objects (snapshot)
            val shieldSnapshot = engine.state.shieldObjects.toList()
            shieldSnapshot.forEach { obj ->
                val center = Offset(obj.x, obj.y - obj.size * 0.5f)
                val radius = obj.size
                drawCircle(
                    color = Color.Black,
                    radius = radius + 4f,
                    center = center,
                    style = Stroke(width = 6f)
                )
                drawImage(
                    image = shieldBitmap,
                    dstOffset = IntOffset(
                        (obj.x - obj.size).toInt(),
                        (obj.y - obj.size * 1.5f).toInt()
                    ),
                    dstSize = IntSize(obj.size.toInt() * 2, obj.size.toInt() * 2)
                )
            }

            // item objects (snapshot)
            engine.state.itemObjects.toList().forEach { obj ->
                drawCircle(
                    color = Color(0x66FFD700),
                    radius = obj.size * 2f,
                    center = Offset(obj.x, obj.y)
                )
                drawCircle(
                    color = Color.DarkGray,
                    radius = obj.size,
                    center = Offset(obj.x, obj.y)
                )
            }

            // ---------- releaseEffects (vẽ từ snapshot) ----------
            val now = System.currentTimeMillis()
            releaseEffects.forEach { effect ->
                val progress = (now - effect.startTime) / effect.duration.toFloat()
                if (progress < 1f) {
                    val radius = playerRadius * (1f + 4f * progress)
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0x66FFD700).copy(alpha = alpha),
                        radius = radius,
                        center = if (effect.isPlayer) Offset(engine.state.player.x, engine.state.player.y) else effect.center
                    )
                }
            }
            // Xoá các effect đã hết
            releaseEffects.removeAll { now - it.startTime >= it.duration }



            // ---------- Ulti của player (render dựa trên flag engine) ----------
            // Lưu ý: quản lý bật/tắt ulti (engine.isUlti, engine.ultiStart, engine.ultiDuration) phải ở GameEngine
            if (engine.isUlti) {
                val elapsed = System.currentTimeMillis() - engine.ultiStart
                val progress = (elapsed.toFloat() / engine.ultiDuration).coerceIn(0f, 1f)
                val maxRadius = size.maxDimension * 1.2f
                val radius = maxRadius * progress
                val alpha = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.Cyan.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(engine.state.player.x, engine.state.player.y),
                    style = Stroke(width = 64f * (1f - progress))
                )
                // *KHÔNG* set engine.isUlti = false ở đây — để engine quản lý trạng thái
            }

            // ---------- Player trail ----------
            if (!engine.state.isGameOver) {
                playerTrail.add(Offset(engine.state.player.x, engine.state.player.y) to now)
                playerTrail.removeAll { now - it.second > trailDuration }
            } else {
                playerTrail.clear()
            }
            val trailSnapshot = playerTrail.toList()
            trailSnapshot.forEach { (pos, time) ->
                val age = (now - time).toFloat() / trailDuration
                val alpha = (1f - age).coerceIn(0f, 1f)

                // Kéo trail ra sau tỉ lệ thuận với tốc độ obstacle
                val speedFactor = engine.obstacleSpeed * 25f  // hệ số nhân để điều chỉnh độ kéo
                val shifted = pos.copy(x = pos.x - age * speedFactor)

                drawCircle(
                    color = Color.DarkGray.copy(alpha = alpha * (1f - age * 1.75f)),
                    radius = 30f * (1f - age * 0.75f),
                    center = shifted
                )
            }


            // ---------- Vẽ player ----------
            if (chargeProgress > 0f && !engine.isUlti) {
                drawCircle(
                    color = blendedColor,
                    radius = playerRadius * chargeProgress * 2f,
                    center = Offset(engine.state.player.x, engine.state.player.y)
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 1f - 0.3f * chargeProgress),
                radius = playerRadius * (1f + 0.3f * chargeProgress),
                center = Offset(engine.state.player.x, engine.state.player.y)
            )

            // Cooldown bar
            if (engine.state.gameType == Types.JUMP) {
                val barWidth = 100f
                val barHeight = 10f
                val fraction = engine.getUltiCooldownInverseFraction()

                val barTopLeft = Offset(engine.state.player.x - barWidth / 2, engine.state.player.y - playerRadius - 40f)

                // Vẽ background bar
                drawRect(
                    color = Color.Gray.copy(alpha = 0.5f),
                    topLeft = barTopLeft,
                    size = Size(barWidth, barHeight)
                )

                // Vẽ phần cooldown hiện tại
                drawRect(
                    color = Color.Black,
                    topLeft = barTopLeft,
                    size = Size(barWidth * fraction, barHeight)
                )

                // Nếu không còn cooldown → vẽ viền vàng toả sáng
                if (fraction <= 0f) {
                    val glowWidth = 6f
                    val elapsed = System.currentTimeMillis() % 1000L
                    val glowAlpha = 0.5f + 0.5f * kotlin.math.sin(elapsed / 1000.0 * 2 * Math.PI).toFloat() // nhấp nháy
                    drawRect(
                        color = Color.Yellow.copy(alpha = glowAlpha),
                        topLeft = barTopLeft,
                        size = Size(barWidth, barHeight),
                        style = Stroke(width = glowWidth)
                    )
                }
            }


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

            restore()
        }
    }
}