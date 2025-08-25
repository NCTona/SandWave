package com.tona.sandwave.engine

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.sandwave.model.GameState
import com.tona.sandwave.model.Obstacle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import android.content.Context
import androidx.compose.ui.platform.LocalContext

class GameEngine(val canvasWidth: Float, val canvasHeight: Float, context: Context) {

    val state = GameState()
    val sound = Sound(context = context)

    // offset "kéo" sóng sang trái (mặt đất chuyển động)
    var waveOffset by mutableStateOf(0f)

    var isHolding by mutableStateOf(false) // nhấn giữ

    private val obstacleSpeedNormal = 3.5f
    private val obstacleSpeedBoost = 8f
    private val gravityNormal = 0.4f
    private val gravityReduced = 0.2f

    private var obstacleSpeed = 3.5f
    private val playerRadius = 30f
    private var gravity = 0.4f
    private val jumpVelocity = 15f

    private var lastTime = System.nanoTime()

    // obstacle spawn control
    private var lastSpawnTime = System.currentTimeMillis()
    private var nextSpawnDelay = Random.nextLong(2500, 4500)

    // =========================
    // WAVE = baseline(x) + Σ Ai * sin(2π * x / λi + φi)
    // =========================

    // Sóng nền (baseline) – thay đổi rất chậm để độ sâu thung lũng khác nhau
    private val baseY = canvasHeight / 2f
    private val baselineAmp = Random.nextFloat() * 40f + 30f                 // 30..70
    private val baselineLambda = canvasWidth * (Random.nextFloat() + 2.5f)   // ~ 2.5..3.5 màn hình
    private val baselinePhase = Random.nextFloat() * (2 * PI).toFloat()

    // Thành phần chính (wavelength ~ 1/2 .. 2/3 màn hình)
    private val mainAmp = Random.nextFloat() * 50f + 60f                     // 60..110
    private val mainLambda = (canvasWidth / 2f) + Random.nextFloat() * (canvasWidth * 2f / 3f - canvasWidth / 2f)
    private val mainPhase = Random.nextFloat() * (2 * PI).toFloat()

    // Hai thành phần phụ để tạo độ uốn lượn đa dạng (biên độ nhỏ hơn, tần số cao hơn)
    private val subAmp1 = mainAmp * (0.35f + Random.nextFloat() * 0.25f)     // ~0.35..0.60 của main
    private val subLambda1 = mainLambda * (0.55f + Random.nextFloat() * 0.25f) // ~0.55..0.80 của main
    private val subPhase1 = Random.nextFloat() * (2 * PI).toFloat()

    private val subAmp2 = mainAmp * (0.20f + Random.nextFloat() * 0.20f)     // ~0.20..0.40 của main
    private val subLambda2 = mainLambda * (0.33f + Random.nextFloat() * 0.20f) // ~0.33..0.53 của main
    private val subPhase2 = Random.nextFloat() * (2 * PI).toFloat()

    private val minWaveY = canvasHeight * 0.45f   // sâu nhất
    private val maxWaveY = canvasHeight * 0.65f   // cao nhất

    init {
        state.reset()
        updatePlayer((System.nanoTime() - lastTime) / 1_000_000_000f)
    }

    fun update() {
        if (state.isGameOver) return

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
        lastTime = currentTime

        if (!isHolding || !state.player.isJumping) obstacleSpeed = obstacleSpeedNormal
        if (!isHolding || !state.player.isJumping) gravity = gravityNormal

        // Sóng "chảy" qua người chơi
        waveOffset += obstacleSpeed

        updatePlayer(deltaTime)
        updateObstacles()
        maybeSpawnObstacle()
        checkCollisions()
    }

    fun reset (){
        state.reset()
        waveOffset = 0f
        updatePlayer((System.nanoTime() - lastTime) / 1_000_000_000f)
    }

    // =========================
    // WAVE HEIGHT (thế giới): x là worldX (không phải screenX)
    // =========================
    private fun waveAt(worldX: Float): Float {
        val x = worldX.toDouble()

        // baseline rất chậm
        val base = baseY + baselineAmp * sin((2.0 * PI * x) / baselineLambda + baselinePhase)

        // thành phần chính + phụ
        val rawY =
            base +
                    mainAmp * sin((2.0 * PI * x) / mainLambda + mainPhase) +
                    subAmp1 * sin((2.0 * PI * x) / subLambda1 + subPhase1) +
                    subAmp2 * sin((2.0 * PI * x) / subLambda2 + subPhase2)

        // scale để không vượt quá min/max
        val normalized = (rawY - baseY) / (mainAmp + subAmp1 + subAmp2 + baselineAmp) // -1..1
        return (baseY + normalized * (maxWaveY - minWaveY) / 2f).toFloat()
    }

    // API public cho Canvas: truyền vào worldX = screenX + waveOffset
    fun getWaveHeightAt(worldX: Float): Float = waveAt(worldX)

    // =========================
    // Player
    // =========================
    private fun updatePlayer(deltaTime: Float) {
        val player = state.player
        val baseY = waveAt(player.x + waveOffset)

        if (player.isJumping) {
            player.y -= player.velocityY * deltaTime * 60f
            player.velocityY -= gravity * deltaTime * 60f
            if (player.y + playerRadius > baseY) {
                player.y = baseY - playerRadius
                player.isJumping = false
                player.velocityY = 0f
            }
        } else {
            player.y = baseY - playerRadius
        }
    }

    fun playerJump() {
        if (!state.player.isJumping) {
            state.player.velocityY = jumpVelocity
            state.player.isJumping = true
        }
        sound.playJumpSound()
    }

    fun playerFly(){
        obstacleSpeed = if (state.player.isJumping && isHolding) obstacleSpeedBoost else obstacleSpeedNormal
        gravity = if (state.player.isJumping && isHolding) gravityReduced else gravityNormal
    }

    // =========================
    // Obstacles: bám mặt sóng
    // =========================
    private fun updateObstacles() {
        // di chuyển sang trái trong toạ độ màn hình
        state.obstacles.forEach { obs ->
            obs.x -= obstacleSpeed
            // cập nhật Y theo mặt sóng tại worldX tương ứng
            val worldX = obs.x + waveOffset
            val waveY = waveAt(worldX)
            obs.y = waveY - obs.height
        }
        // xoá khi ra khỏi màn
        state.obstacles.removeAll { it.x + it.width < -1000f }
    }

    private fun maybeSpawnObstacle() {
        val now = System.currentTimeMillis()
        if (now - lastSpawnTime > nextSpawnDelay) {
            // Chọn vị trí spawn theo toạ độ thế giới rồi quy về toạ độ màn hình
            val spawnWorldX = waveOffset + canvasWidth + Random.nextInt(300, 600)
            val waveY = waveAt(spawnWorldX)
            val obsHeight = 40f + Random.nextFloat() * 60f

            state.obstacles.add(
                Obstacle(
                    x = spawnWorldX - waveOffset,  // lưu theo toạ độ màn hình
                    y = waveY - obsHeight,         // đáy obstacle chạm sóng
                    width = 40f,
                    height = obsHeight
                )
            )
            lastSpawnTime = now
            nextSpawnDelay = Random.nextLong(2500, 4500)
        }
    }

    // =========================
    // Collision
    // =========================
    private fun checkCollisions() {
        val player = state.player
        val playerTop = player.y - playerRadius
        val playerBottom = player.y + playerRadius

        state.obstacles.forEach { obs ->
            val obsTop = obs.y
            val obsBottom = obs.y + obs.height

            if (player.x + playerRadius > obs.x &&
                player.x - playerRadius < obs.x + obs.width &&
                playerBottom > obsTop && playerTop < obsBottom
            ) {
                sound.playCrashSound()
                state.isGameOver = true
            }
        }
    }
}
