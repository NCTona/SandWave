package com.tona.sandwave.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.sandwave.model.GameState
import com.tona.sandwave.model.Obstacle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import android.content.Context
import android.util.Log
import com.tona.sandwave.model.Projectile
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

class GameEngine(val canvasWidth: Float, val canvasHeight: Float, context: Context) {

    val state = GameState()
    val sound = Sound(context = context)

    // offset "kéo" sóng sang trái (mặt đất chuyển động)
    var waveOffset by mutableStateOf(0f)

    var isHolding by mutableStateOf(false) // nhấn giữ

    private var obstacleSpeedNormal = 4f
    private var gravityNormal = 0.4f
    private var gravityReduced = 0.15f

    var obstacleSpeed = 3.5f
    private val playerRadius = 30f
    private var gravity = 0.4f
    private val jumpVelocity = 15f

    val pendingProjectiles = mutableListOf<Projectile>()

    // Hệ số điểm
    private val pixelsPerPoint = 10f  // tuỳ chỉnh độ nhanh tăng điểm

    private var lastTime = System.nanoTime()

    // obstacle spawn control
    private var lastSpawnTime = System.currentTimeMillis()
    private var nextSpawnDelay = Random.nextLong(2500, 4500)

    // falling object spawn control
    private var lastFallingSpawnTime = System.currentTimeMillis()
    private var nextFallingSpawnDelay = Random.nextLong(3000, 6000)

    // Tổng hợp các vị trí va chạm để UI đọc và tạo shockwave (thread-safe)
    val pendingShockwaves = ConcurrentLinkedQueue<Pair<Float, Float>>() // (x, y)


    // =========================
    // WAVE = baseline(x) + Σ Ai * sin(2π * x / λi + φi)
    // =========================

    // Sóng nền (baseline) – thay đổi rất chậm để độ sâu thung lũng khác nhau
    private val baseY = canvasHeight / 1.5f
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

        Log.d("GameEngine", state.player.isJumping.toString())

        obstacleSpeed = (obstacleSpeed + 0.0005f).coerceAtMost(10f)
        obstacleSpeedNormal = (obstacleSpeedNormal + 0.0005f).coerceAtMost(10f)

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
        lastTime = currentTime

        if (!isHolding || !state.player.isJumping) obstacleSpeed = obstacleSpeedNormal
        if (!isHolding && !state.player.isJumping) gravity = gravityNormal

        // Gọi giảm gravity khi đang hold
        playerAccumulate()

        // Sóng "chảy" qua người chơi
        waveOffset += obstacleSpeed

        // --- TÍNH ĐIỂM ---
        // Điểm = tổng quãng đường đã đi (pixel) / pixelsPerPoint
        val newScore = (waveOffset / pixelsPerPoint).toLong()
        if (newScore != state.score) state.score = newScore

        updatePlayer(deltaTime)
        updateObstacles()
        updateFallingObjects()  // cập nhật nhiều falling object
        maybeSpawnFallingObject()  // spawn nhiều falling object
        maybeSpawnObstacle()
        checkCollisions()
        updateProjectiles()
    }

    fun reset() {
        state.reset()
        isHolding = false
        waveOffset = 0f
        obstacleSpeedNormal = 4f
        obstacleSpeed = 3.5f
        state.score = 0L
        state.fallingObjects.clear() // xoá tất cả object
        pendingShockwaves.clear()
        updatePlayer((System.nanoTime() - lastTime) / 1_000_000_000f)
    }


    // =========================
    // WAVE HEIGHT
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
            sound.playJumpSound()
        }
    }

    fun playerAccumulate(){
        if (!state.player.isJumping && isHolding) {
            // Khi đang hold, giảm gravity dần dần
            gravity = (gravity - 0.002f).coerceAtLeast(gravityReduced)
        }
    }

    fun playerUlti(holdTime: Float) {
        val player = state.player
        if (!player.isJumping) return
        val minHold = 0.1f
        if (holdTime < minHold) return

        val maxPower = 100f
        val power = (holdTime * 100f).coerceAtMost(maxPower)

        if(player.isJumping){
            pendingProjectiles.add(
                Projectile(
                    x = player.x,
                    y = player.y,
                    speedX = 10f + power / 2f,
                    speedY = 0f,
                    radius = 24f + power / 1.5f
                )
            )
        }
    }


    // =========================
    // Projectile: quả cầu
    // =========================
    private fun updateProjectiles() {

        if (pendingProjectiles.isNotEmpty()) {
            state.projectiles.addAll(pendingProjectiles)
            pendingProjectiles.clear()
        }

        val iterator = state.projectiles.iterator()
        while (iterator.hasNext()) {
            val proj = iterator.next()
            proj.x += proj.speedX
            proj.y += proj.speedY
//            proj.speedY += 0.2f  // gravity

            // --- Kiểm tra va chạm obstacles ---
            val obsIterator = state.obstacles.iterator()
            var hit = false
            while (obsIterator.hasNext()) {
                val obs = obsIterator.next()
                if (proj.x + proj.radius > obs.x &&
                    proj.x - proj.radius < obs.x + obs.width &&
                    proj.y + proj.radius > obs.y &&
                    proj.y - proj.radius < obs.y + obs.height
                ) {
                    pendingShockwaves.offer(Pair(proj.x, proj.y))
                    iterator.remove()
                    obsIterator.remove()
                    hit = true
                    break
                }
            }
            if (hit) continue

            // --- Kiểm tra va chạm falling objects ---
            val fallIterator = state.fallingObjects.iterator()
            while (fallIterator.hasNext()) {
                val obj = fallIterator.next()
                val dx = proj.x - obj.x
                val dy = proj.y - obj.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance < proj.radius + obj.size) {
                    pendingShockwaves.offer(Pair(proj.x, proj.y))
                    iterator.remove()
                    fallIterator.remove()
                    hit = true
                    break
                }
            }
            if (hit) continue

            // --- Xoá nếu ra khỏi màn ---
            if (proj.x > canvasWidth + 2000 || proj.y > canvasHeight + 200 || proj.x < -200) {
                iterator.remove()
            }
        }
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
            val spawnWorldX = waveOffset + canvasWidth + Random.nextInt(300, 600) + 2500
            val waveY = waveAt(spawnWorldX)
            val obsHeight = 50f + Random.nextFloat() * 100f

            state.obstacles.add(
                Obstacle(
                    x = spawnWorldX - waveOffset,
                    y = waveY - obsHeight - 20f,
                    width = 40f,
                    height = obsHeight
                )
            )
            lastSpawnTime = now
            nextSpawnDelay = Random.nextLong(2500, 4500)
        }
    }

    // =========================
    // Falling Objects: rơi chéo xuống từ góc phải
    // =========================
    private fun updateFallingObjects() {
        val iterator = state.fallingObjects.iterator()
        val player = state.player
        val now = System.currentTimeMillis()
        while (iterator.hasNext()) {
            val obj = iterator.next()

            // cập nhật vị trí (screen coordinates)
            obj.x += obj.speedX
            obj.y += obj.speedY

            // --- Kiểm tra va chạm với player ---
            val dx = player.x - obj.x
            val dy = player.y - obj.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < playerRadius + obj.size) {
                // Va chạm player
                // đẩy vị trí va chạm (obj.x, obj.y) vào queue để UI vẽ shockwave
                pendingShockwaves.offer(Pair(obj.x, obj.y))
                continue
            }

            // tính height của sóng tại vị trí object (dùng worldX = screenX + waveOffset)
            val waveY = waveAt(obj.x + waveOffset)
            // chạm sóng: đáy object chạm wave
            if (obj.y + obj.size >= waveY) {
                // đẩy vị trí va chạm (obj.x, waveY) vào queue để UI vẽ shockwave
                pendingShockwaves.offer(Pair(obj.x, waveY))
                iterator.remove()
                continue
            }

            // xoá khi ra khỏi màn hình
            if (obj.x < -100f || obj.y > canvasHeight + 100f) {
                iterator.remove()
            }
        }
    }


    private fun maybeSpawnFallingObject() {
        val now = System.currentTimeMillis()
        if (now - lastFallingSpawnTime > nextFallingSpawnDelay) {
            // spawn ở góc phải trên (screen coords)
            val spawnCount = Random.nextInt(0, 5) // spawn 0..5 object cùng lúc
            repeat(spawnCount) {
                val spawnX = canvasWidth + 50f + Random.nextFloat() * 1000f
                val spawnY = -150f - Random.nextFloat() * 100f

                state.fallingObjects.add(
                    com.tona.sandwave.model.FallingObject(
                        x = spawnX,
                        y = spawnY,
//                        speedX = -(10..14).random().toFloat(), // chéo sang trái
//                        speedY = (6..8).random().toFloat()  // rơi xuống
                        speedX = -12f,
                        speedY = 6f,
                    )
                )
            }

            lastFallingSpawnTime = now
            nextFallingSpawnDelay = Random.nextLong(2500, 5000)
        }
    }

    // =========================
    // Collision
    // =========================
    private fun checkCollisions() {
        val player = state.player
        val playerTop = player.y - playerRadius
        val playerBottom = player.y + playerRadius
        val playerLeft = player.x - playerRadius
        val playerRight = player.x + playerRadius

        // --- Va chạm obstacles ---
        state.obstacles.forEach { obs ->
            val obsTop = obs.y
            val obsBottom = obs.y + obs.height
            val obsLeft = obs.x
            val obsRight = obs.x + obs.width

            if (playerRight > obsLeft &&
                playerLeft < obsRight &&
                playerBottom > obsTop &&
                playerTop < obsBottom
            ) {
                sound.playCrashSound()
                state.isGameOver = true
            }
        }

        // --- Va chạm falling objects ---
        val fallIterator = state.fallingObjects.iterator()
        while (fallIterator.hasNext()) {
            val obj = fallIterator.next()
            val dx = player.x - obj.x
            val dy = player.y - obj.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < playerRadius + obj.size && (!isHolding || state.player.isJumping)) {
                // Player bị trúng falling object
                sound.playCrashSound()
                state.isGameOver = true
                // Nếu muốn, cũng có thể xóa object khi va chạm
                // fallIterator.remove()
            } else if (distance < playerRadius + obj.size && isHolding) {
                 fallIterator.remove()
            }
        }
    }
}
