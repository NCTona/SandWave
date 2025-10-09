package com.tona.balladventure.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.balladventure.model.GameState
import com.tona.balladventure.model.Obstacle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import android.content.Context
import android.util.Log
import com.tona.balladventure.audio.MusicPlayer
import com.tona.balladventure.level.LevelConfig
import com.tona.balladventure.model.Enemy
import com.tona.balladventure.model.Wave
import com.tona.balladventure.type.Types
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.pow

class GameEngine(val canvasWidth: Float, val canvasHeight: Float, context: Context) {

    // =========================
    // TRẠNG THÁI CHUNG CỦA GAME
    // =========================
    val state = GameState()          // Lưu trạng thái player, obstacles, score, coin, v.v.
    val sound = Sound(context = context)       // Quản lý âm thanh ngắn
    val musicPlayer = MusicPlayer(context)    // Quản lý nhạc nền

    // =========================
    // TRẠNG THÁI PLAYER
    // =========================
    var isOnTopWave = false
    var isHolding by mutableStateOf(false)          // Nhấn giữ (hold) để giảm gravity
    var isHoldingShield by mutableStateOf(false)    // Player đang giữ shield
    var shieldExpireTime: Long = 0L                 // Thời gian kết thúc shield

    private val playerRadius = 30f                  // Bán kính player (không đổi)

    // =========================
    // VẬN TỐC & GRAVITY
    // =========================
    var obstacleSpeedNormal: Float = 0f          // Vận tốc obstacle normal, set từ LevelConfig
    var obstacleSpeedFastMax: Float = 0f          // Vận tốc obstacle normal, set từ LevelConfig
    var obstacleSpeed: Float = 0f       // Vận tốc obstacle, set từ LevelConfig
    private var gravityNormal: Float = 0f   // Gravity khi không hold, set từ LevelConfig
    private var gravityReduced: Float = 0f  // Gravity khi hold, set từ LevelConfig
    private var gravity: Float = 0f         // Gravity hiện tại đang áp dụng
    private var jumpVelocity: Float = 0f    // Vận tốc nhảy player, set từ LevelConfig

    // =========================
    // HỆ SỐ TÍNH ĐIỂM
    // =========================
    private var pixelsPerPoint: Float = 0f   // Số pixel tương ứng 1 điểm, set từ LevelConfig

    // =========================
    // CONTROL SPAWN CÁC OBJECT
    // =========================
    private var lastTime = System.nanoTime()   // Thời gian update frame trước

    // --- Obstacle spawn ---
    private var lastSpawnTime = System.currentTimeMillis()
    private var nextSpawnDelay: Long = 0L     // Thời gian delay spawn tiếp theo, lấy ngẫu nhiên trong range từ LevelConfig

    // --- Falling object spawn ---
    private var lastFallingSpawnTime = System.currentTimeMillis()
    private var nextFallingSpawnDelay: Long = 0L

    // --- Item object spawn ---
    private var lastItemSpawnTime = System.currentTimeMillis()
    private var nextItemSpawnDelay: Long = 0L

    // --- Shield object spawn ---
    private var lastShieldSpawnTime = System.currentTimeMillis()
    private var nextShieldSpawnDelay: Long = 0L

    // --- Enemy spawn ---
    private var lastEnemySpawnTime = System.currentTimeMillis()
    private var nextEnemySpawnDelay: Long = 0L

    // =========================
    // ULTI / SKILL
    // =========================
    private var lastUltiTime = 0L      // Thời gian sử dụng ulti trước đó
    private var ultiCooldown: Long = 0L // Cooldown ulti, set từ LevelConfig
    var isUlti by mutableStateOf(false) // Player đang dùng ulti
    var ultiDuration: Long = 0L        // Thời gian hiệu lực ulti, set từ LevelConfig
    var ultiStart = 0L                  // Thời gian bắt đầu ulti

    // --- Switch cooldown
    private val switchCooldown = 200L           // thời gian giữa hai lần switch (ms)
    private var lastSwitchTime = 0L

    // =========================
    // TRẠNG THÁI SÓNG
    // =========================
    var waveOffset by mutableStateOf(0f)  // Offset kéo sóng sang trái (mặt đất di chuyển)

    // =========================
    // DỮ LIỆU SHOCKWAVE CHO UI
    // =========================
    val pendingShockwaves = ConcurrentLinkedQueue<Pair<Float, Float>>() // Danh sách vị trí va chạm để UI vẽ shockwave


    var waveData = Wave.random(canvasWidth, canvasHeight)
    private val gapTeeth = mutableMapOf<Pair<Float, Float>, List<Float>>()


    init {
        state.reset()
        importLevelData(level = state.levelConfig)
        resetPlayerPosition()
    }

    fun importLevelData(level: LevelConfig) {
        obstacleSpeedNormal = level.obstacleSpeedNormal
        obstacleSpeedFastMax = level.obstacleSpeedFastMax
        obstacleSpeed = level.obstacleSpeed
        gravity = level.gravity
        gravityNormal = level.gravityNormal
        gravityReduced = level.gravityReduced
        jumpVelocity = level.jumpVelocity
        pixelsPerPoint = level.pixelsPerPoint
        nextSpawnDelay = Random.nextLong(level.nextSpawnDelayRange.first, level.nextSpawnDelayRange.last)
        nextFallingSpawnDelay = Random.nextLong(level.nextFallingDelayRange.first, level.nextFallingDelayRange.last)
        nextItemSpawnDelay = Random.nextLong(level.nextItemDelayRange.first, level.nextItemDelayRange.last)
        nextShieldSpawnDelay = Random.nextLong(level.nextShieldDelayRange.first, level.nextShieldDelayRange.last)
        nextEnemySpawnDelay = Random.nextLong(level.nextEnemyDelayRange.first, level.nextEnemyDelayRange.last)
        ultiCooldown = level.ultiCooldown
        ultiDuration = level.ultiDuration
    }

    private var pauseStartTime: Long = 0L      // thời điểm bắt đầu pause
    private var pausedDuration: Long = 0L      // tổng thời gian pause (nano)

    fun update() {
        if (state.isGameOver) return

        val currentTime = System.nanoTime()

        // --- XỬ LÝ TIME PAUSE ---
        if (state.isPaused) {
            // nếu vừa bắt đầu pause
            if (pauseStartTime == 0L) {
                pauseStartTime = currentTime
            }
            // cộng thêm thời gian pause kể từ frame trước
            pausedDuration += currentTime - pauseStartTime
            pauseStartTime = currentTime

            return  // === dừng update game logic khi pause ===
        } else {
            // khi resume: reset pauseStartTime để chuẩn bị lần pause tiếp theo
            pauseStartTime = 0L
        }

        // --- TÍNH DELTA TIME ---
        val deltaTime = (currentTime - lastTime - pausedDuration) / 1_000_000_000f
        lastTime = currentTime
        pausedDuration = 0L  // reset sau khi tính xong

        // -------------------------
        // UPDATE LOGIC GAME
        // -------------------------
        if (!isHolding && !state.player.isJumping) gravity = gravityNormal

        if (isHoldingShield && System.currentTimeMillis() > shieldExpireTime) {
            isHoldingShield = false
        }

        Log.d("GameEngine", "obstacleSpeed: $obstacleSpeed")

        obstacleSpeedNormal += 0.00005f.coerceAtMost(5f)

        // Giảm obstacleSpeed dần về normal sau khi ulti
        if (obstacleSpeed > obstacleSpeedNormal) {
            val decayRate = 0.005f // tốc độ giảm (càng lớn giảm càng nhanh)
            obstacleSpeed = (obstacleSpeed - decayRate).coerceAtLeast(obstacleSpeedNormal)
        }

        if (obstacleSpeed < obstacleSpeedNormal) {
            val decayRate = 0.005f // tốc độ giảm (càng lớn giảm càng nhanh)
            obstacleSpeed = (obstacleSpeed + decayRate).coerceAtMost(obstacleSpeedNormal)
        }

        playerAccumulate()

        waveOffset += obstacleSpeed
        state.distance = waveOffset / pixelsPerPoint

        val newScore = (waveOffset / pixelsPerPoint).toLong()
        state.score = newScore + state.coin * 100

        updatePlayer(deltaTime)
        updateObstacles()
        updateItemObjects()
        updateFallingObjects()
        updateShieldObjects()
        updateEnemies()
        maybeSpawnFallingObject()
        maybeSpawnItemObject()
        maybeSpawnShieldObject()
        maybeSpawnObstacle()
        maybeSpawnEnemy()
        checkCollisions()
        checkUltiCollisions()
        checkFallTrap()
    }


    fun reset() {
        state.reset()
        importLevelData(level = state.levelConfig)
        waveData = Wave.random(canvasWidth, canvasHeight)
        gapTeeth.clear()
        isHolding = false
        waveOffset = 0f
        state.score = 0L
        state.coin = 0
        state.distance = 0f
        state.fallingObjects.clear() // xoá tất cả object
        pendingShockwaves.clear()
        resetPlayerPosition()
    }



    // =========================
    // WAVE HEIGHT (BOTTOM)
    // =========================
    private fun waveAt(worldX: Float): Float {
        val x = worldX.toDouble()

        // Sóng dưới - dao động nhẹ, nằm ở 3/4 màn hình
        val baseY = canvasHeight * 0.8f

        val rawY =
            baseY +
                    waveData.mainAmp * 0.6f * sin((2.0 * PI * x) / waveData.mainLambda + waveData.mainPhase) +
                    waveData.subAmp1 * 0.5f * sin((2.0 * PI * x) / waveData.subLambda1 + waveData.subPhase1) +
                    waveData.subAmp2 * 0.4f * sin((2.0 * PI * x) / waveData.subLambda2 + waveData.subPhase2)

        var adjustedY = rawY.toFloat()

        // Áp dụng vùng bẫy (giữ nguyên)
        if (state.gameType == Types.JUMP){
            for ((start, end) in waveData.waveGaps) {
                if (worldX in start..end) {
                    val teeth = gapTeeth.getOrPut(start to end) {
                        val count = ((end - start) / 20f).toInt().coerceAtLeast(6)
                        List(count) { Random.nextFloat() * 1.4f + 0.6f }
                    }

                    val gapCenter = (start + end) / 2f
                    val gapHalf = (end - start) / 2f
                    val distToCenter = abs(worldX - gapCenter)
                    val normalizedDist = (distToCenter / gapHalf).coerceIn(0f, 1f)

                    val flatZone = 0.3f
                    val curveSoftness = 3.2f
                    val smoothIntensity = when {
                        normalizedDist < flatZone -> 1f
                        else -> ((1f - normalizedDist) / (1f - flatZone))
                            .coerceIn(0f, 1f)
                            .pow(curveSoftness)
                    }

                    val trapYBase = canvasHeight * 1.02f
                    val trapWidth = end - start
                    val localX = ((worldX - start) / trapWidth) * teeth.size
                    val index = localX.toInt().coerceIn(0, teeth.size - 1)
                    val nextIndex = (index + 1).coerceAtMost(teeth.size - 1)
                    val frac = localX - index
                    val heightFactor = teeth[index] * (1 - frac) + teeth[nextIndex] * frac
                    val baseAmp = 100f
                    val teethAmp = baseAmp * heightFactor
                    val waveScale = 5
                    val wave = sin(localX * 2 * PI / waveScale)
                    val teethOffset = -abs(wave) * teethAmp
                    val teethFade = smoothIntensity.pow(2.5f)

                    val trapYWithTeeth = trapYBase + teethOffset * teethFade
                    val blend = smoothIntensity.pow(1.8f)
                    adjustedY = (rawY * (1f - blend) + trapYWithTeeth * blend).toFloat()
                    break
                }
            }
        }

        return adjustedY.coerceIn(0f, canvasHeight)
    }

    fun getWaveHeightAt(worldX: Float): Float = waveAt(worldX)

    // =========================
    // WAVE HEIGHT (TOP)
    // =========================
    private fun waveTopAt(worldX: Float): Float {
        val x = worldX.toDouble()

        val base = waveData.baseY * 0.35f + waveData.baselineAmp * 0.55f *
                sin((2.0 * PI * x) / (waveData.baselineLambda * 1.1) + waveData.baselinePhase + Math.PI)

        val rawY =
            base - waveData.mainAmp * 0.6f * sin((2.0 * PI * x) / (waveData.mainLambda * 1.05) + waveData.mainPhase + 0.9) -
                    waveData.subAmp1 * 0.4f * sin((2.0 * PI * x) / (waveData.subLambda1 * 0.95) + waveData.subPhase1 + 1.3) -
                    waveData.subAmp2 * 0.3f * sin((2.0 * PI * x) / (waveData.subLambda2 * 1.15) + waveData.subPhase2 + 0.5)

        val normalized = (rawY - waveData.baseY * 0.35f) /
                ((waveData.mainAmp + waveData.subAmp1 + waveData.subAmp2 + waveData.baselineAmp) * 0.6f)

        val finalY = (waveData.baseY * 0.35f -
                normalized * (waveData.maxWaveY - waveData.minWaveY) * 0.45f).toFloat()

        // Đẩy sóng lên trên một chút (thay vì xuống)
        val offset = -canvasHeight * 0.05f // ↑↑↑ đẩy lên thêm 15% chiều cao
        val adjustedY = finalY + offset

        return adjustedY.coerceIn(0f, canvasHeight)
    }

    fun getWaveTopHeightAt(worldX: Float): Float = waveTopAt(worldX)


    // =========================
    // Player
    // =========================
    private fun updatePlayer(deltaTime: Float) {
        val player = state.player

        when (state.gameType.name) {
            "SWITCH" -> {
                val bottomY = waveAt(player.x + waveOffset) - playerRadius
                val topY = getWaveTopHeightAt(player.x + waveOffset) + playerRadius

                val targetY = if (isOnTopWave) topY else bottomY
                val smoothSpeed = 0.04f

                // Di chuyển mượt
                player.y += (targetY - player.y) * smoothSpeed

                // Khi gần chạm mặt sóng → bám theo mặt sóng
                val distanceToWave = abs(targetY - player.y)
                val threshold = 2f // khoảng cách nhỏ để bám sóng
                if (distanceToWave <= threshold) {
                    player.y = targetY
                }

                player.isJumping = false
                player.velocityY = 0f
            }

            "FLY" -> {
                // Player bay tự do
                player.y += player.velocityY * deltaTime * 60f

                // Ma sát nhẹ để dừng dần khi không giữ
                player.velocityY *= 0.95f

                // Giới hạn trong màn hình
                val minY = playerRadius
                val maxY = canvasHeight - playerRadius
                if (player.y < minY) {
                    player.y = minY
                    player.velocityY = 0f
                }
                if (player.y > maxY) {
                    player.y = maxY
                    player.velocityY = 0f
                }
                return
            }

            "JUMP" -> {
                // Chế độ JUMP mặc định
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
        }
    }


    fun resetPlayerPosition() {
        val player = state.player
        player.velocityY = 0f
        player.isJumping = false

        when (state.gameType) {
            Types.SWITCH, Types.JUMP -> {
                val baseY = waveAt(player.x + waveOffset)
                player.y = baseY - playerRadius
            }
            Types.FLY -> {
                player.y = canvasHeight / 2f // giữa màn hình
            }
        }
    }


    fun playerSwitch() {
        if (state.gameType.name != "SWITCH") return

        // Bỏ cooldown để tap liên tục đảo hướng ngay lập tức
        isOnTopWave = !isOnTopWave
        sound.playJumpSound()
    }


    fun playerFly(targetY: Float) {
        val player = state.player
        if (state.gameType != Types.FLY) return

        val smoothFactor = 0.25f // hệ số mượt (0.1f = chậm, 1.0f = tức thời)
        player.y += (targetY - player.y) * smoothFactor

        // Giới hạn biên
        val minY = playerRadius
        val maxY = canvasHeight - playerRadius
        player.y = player.y.coerceIn(minY, maxY)
    }



    fun playerJump() {
        if (!state.player.isJumping) {
            state.player.velocityY = jumpVelocity
            state.player.isJumping = true
            sound.playJumpSound()
        }
    }

    fun playerAccumulate(){
        if (isHolding) {
            // Khi đang hold, giảm gravity dần dần
            gravity = (gravity - 0.0022f).coerceAtLeast(gravityReduced)
        }
    }

    fun playerUlti(holdTime: Float) {
        val player = state.player
        if (!player.isJumping) return

        val now = System.currentTimeMillis()
        val elapsed = now - lastUltiTime - (pausedDuration / 1_000_000) // trừ thời gian pause (nano -> ms)

        if (elapsed < ultiCooldown) {
            // Đang cooldown, không bắn ulti
            return
        }

        val maxPower = 100f
        val power = (holdTime * 100f).coerceAtMost(maxPower)

        val jumpFactor = power / maxPower
        val dynamicJumpVelocity = jumpVelocity * (0.6f + 0.4f * jumpFactor * 1.2f)

        if (power < maxPower) {
            // Double Jump
            player.velocityY = dynamicJumpVelocity
            player.isJumping = true
            gravity = gravityNormal
            sound.playJumpSound()
            lastUltiTime = now
        } else {
            // Ulti max power
            isUlti = true
            ultiStart = now
            player.velocityY = dynamicJumpVelocity
            player.isJumping = true
            gravity = gravityNormal
            sound.playJumpSound()
            obstacleSpeed = (obstacleSpeed + 2f)
                .coerceAtMost(obstacleSpeedFastMax)
            lastUltiTime = now
        }
    }

    // Trả về giá trị [0f..1f]: 0 = skill sẵn sàng, 1 = vừa sử dụng xong
    fun getUltiCooldownFraction(): Float {
        val now = System.currentTimeMillis()
        val elapsed = now - lastUltiTime - (pausedDuration / 1_000_000) // nano -> ms
        return (elapsed.toFloat() / ultiCooldown).coerceIn(0f, 1f)
    }


    // Có thể trả về đảo ngược nếu muốn fill từ đầy -> rỗng
    fun getUltiCooldownInverseFraction(): Float {
        return 1f - getUltiCooldownFraction()
    }


    // =========================
    // Enemy
    // =========================
    private fun maybeSpawnEnemy() {
        if (state.gameType == Types.FLY) return
        val now = System.currentTimeMillis()
        if (now - lastEnemySpawnTime > nextEnemySpawnDelay) {
            val spawnCount = Random.nextInt(1, 2) // spawn 1..2 enemy
            repeat(spawnCount) {
                val spawnX = canvasWidth + 50f + Random.nextFloat() * 10000f

                // Chọn ngẫu nhiên sóng trên hay sóng dưới
                var useTopWave = Random.nextBoolean()

                if (state.gameType == Types.JUMP) {
                    useTopWave = false
                }
                val waveY = if (useTopWave) waveTopAt(waveOffset + spawnX) else waveAt(waveOffset + spawnX)

                val enemyHeight = 40f + Random.nextFloat() * 50f

                // Nếu trên sóng top, đảo hướng spawn để enemy đi xuống
                val enemyY = if (useTopWave) waveY else waveY - enemyHeight

                state.enemies.add(
                    Enemy(
                        x = spawnX,
                        y = enemyY,
                        width = 40f,
                        height = enemyHeight,
                        speed = obstacleSpeed * 1.5f,
                        size = enemyHeight / 2,
                        isTop = useTopWave // thêm cờ để xử lý riêng (nếu cần)
                    )
                )
            }

            lastEnemySpawnTime = now
        }
    }


    private fun updateEnemies() {
        val iterator = state.enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            enemy.x -= enemy.speed

            val enemyRight = enemy.x + enemy.width
            val enemyBottom = enemy.y + enemy.height

            val worldX = enemy.x + waveOffset
            val waveY = if (enemy.isTop) waveTopAt(worldX) else waveAt(worldX)
            enemy.y = if (enemy.isTop) waveY else waveY - enemy.height

            if (state.gameType == Types.JUMP && isInTrapZone(worldX)) {
                pendingShockwaves.offer(Pair(enemy.x, waveY))
                if (enemyRight >= 0 && enemy.x <= canvasWidth * state.scale &&
                    enemyBottom >= 0 && enemy.y <= canvasHeight * state.scale) {
                    sound.playShockwaveSound()
                }
                iterator.remove()
                continue
            }

            // --- va chạm player ---
            val enemyCenterX = enemy.x + enemy.width / 2
            val enemyCenterY = enemy.y + enemy.height / 2
            val dx = state.player.x - enemyCenterX
            val dy = state.player.y - enemyCenterY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < playerRadius + enemy.size) {
                pendingShockwaves.offer(Pair(enemy.x, enemy.y))
                sound.playShockwaveSound()
            }


            // xoá khi ra ngoài màn hình xa
            if (enemyRight < -1000f) {
                iterator.remove()
            }
        }
    }




    // =========================
    // Obstacles: bám mặt sóng
    // =========================
    private fun updateObstacles() {
        state.obstacles.forEach { obs ->
            obs.x -= obstacleSpeed
            val worldX = obs.x + waveOffset
            val waveY = if (obs.isTop) waveTopAt(worldX) else waveAt(worldX)
            obs.y = if (obs.isTop) waveY else waveY - obs.height
        }
        state.obstacles.removeAll { it.x + it.width < -1000f }
    }


    private fun maybeSpawnObstacle() {
        if (state.gameType == Types.FLY) return

        val now = System.currentTimeMillis()
        if (now - lastSpawnTime > nextSpawnDelay) {
            val spawnWorldX = waveOffset + canvasWidth + 50f + Random.nextFloat() * 10000f

            // ======= Kiểm tra trùng vị trí X =======
            val minGap = 1000f // khoảng cách tối thiểu giữa 2 obstacle (tuỳ chỉnh)
            val hasNearbyObstacle = state.obstacles.any { obs ->
                kotlin.math.abs((obs.x + waveOffset) - spawnWorldX) < minGap
            }

            if (hasNearbyObstacle) {
                // Nếu đã có obstacle gần vị trí spawn, bỏ qua để tránh trùng
                return
            }
            // ======================================

            var useTopWave = Random.nextBoolean()
            if (state.gameType == Types.JUMP) {
                useTopWave = false
            }

            val waveY = if (useTopWave) waveTopAt(spawnWorldX) else waveAt(spawnWorldX)
            val obsHeight = 200f + Random.nextFloat() * 150f
            val obsY = if (useTopWave) waveY + obsHeight else waveY - obsHeight

            state.obstacles.add(
                Obstacle(
                    x = spawnWorldX - waveOffset,
                    y = obsY,
                    width = 250f,
                    height = obsHeight,
                    isTop = useTopWave
                )
            )

            lastSpawnTime = now
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
                sound.playShockwaveSound()
                continue
            }

            val waveY = waveAt(obj.x + waveOffset)
            if (state.gameType != Types.FLY) {
                if (obj.y + obj.size >= waveY) {
                    pendingShockwaves.offer(Pair(obj.x, waveY))
                    // --- Chỉ check va chạm với sóng khi object trong màn hình ---
                    if (obj.x + obj.size >= 0 && obj.x - obj.size <= canvasWidth * state.scale &&
                        obj.y + obj.size >= 0 && obj.y - obj.size <= canvasHeight * state.scale) {
                        sound.playShockwaveSound()
                    }
                    iterator.remove()
                    continue
                }
            }

            // xoá khi ra khỏi màn hình
            if (obj.x < -100f || obj.y > canvasHeight + 100f) {
                iterator.remove()
            }
        }
    }


    private fun maybeSpawnFallingObject() {
        if (state.gameType == Types.SWITCH) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastFallingSpawnTime > nextFallingSpawnDelay) {
            // spawn ở góc phải trên (screen coords)
            val spawnCount = Random.nextInt(1, 5) // spawn 0..5 object cùng lúc
            repeat(spawnCount) {
                val spawnX = canvasWidth + 50f + Random.nextFloat() * 10000f
                val spawnY = -150f - Random.nextFloat() * 100f

                state.fallingObjects.add(
                    com.tona.balladventure.model.FallingObject(
                        x = spawnX,
                        y = spawnY,
//                        speedX = -(10..14).random().toFloat(), // chéo sang trái
//                        speedY = (6..8).random().toFloat()  // rơi xuống
                        speedX = -4.5f * obstacleSpeed,
                        speedY = 5f,
                    )
                )
            }

            lastFallingSpawnTime = now
        }
    }

    // =========================
    // Shield Objects: bám mặt sóng
    // =========================
    private fun updateShieldObjects() {
        val iterator = state.shieldObjects.iterator()
        val player = state.player

        while (iterator.hasNext()) {
            val obj = iterator.next()

            // Di chuyển sang trái
            obj.x -= obstacleSpeed

            // 🔹 Bám theo sóng tương ứng (top hoặc bottom)
            val worldX = obj.x + waveOffset
            val waveY = if (obj.isTop) waveTopAt(worldX) else waveAt(worldX)
            obj.y = if (obj.isTop) waveY + obj.size * 2f else waveY - obj.size

            // --- Kiểm tra va chạm với player ---
            val dx = player.x - obj.x
            val dy = player.y - obj.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < playerRadius + obj.size) {
                pendingShockwaves.offer(Pair(obj.x, obj.y))
                isHoldingShield = true
                shieldExpireTime = System.currentTimeMillis() + 5000L // 5 giây
                sound.playShockwaveSound()
                iterator.remove()
                continue
            }

            // Xoá khi ra khỏi màn
            if (obj.x + obj.size < -100f) {
                iterator.remove()
            }
        }
    }


    private fun maybeSpawnShieldObject() {
        if (state.gameType == Types.FLY) return

        val now = System.currentTimeMillis()
        if (now - lastShieldSpawnTime > nextShieldSpawnDelay) {
            val spawnWorldX = waveOffset + canvasWidth + 50f + Random.nextFloat() * 10000f

            // 🔹 Chọn ngẫu nhiên sóng trên hoặc sóng dưới
            var useTopWave = Random.nextBoolean()

            if (state.gameType == Types.JUMP) {
                useTopWave = false
            }

            val waveY = if (useTopWave) waveTopAt(spawnWorldX) else waveAt(spawnWorldX)

            val size = 50f
            val objY = if (useTopWave) waveY + size else waveY - size

            state.shieldObjects.add(
                com.tona.balladventure.model.ShieldObject(
                    x = spawnWorldX - waveOffset,
                    y = objY,
                    speedX = 0f, // không cần vì mình update bằng obstacleSpeed
                    speedY = 0f,
                    size = size,
                    isTop = useTopWave // thêm cờ đánh dấu sóng nào
                )
            )

            lastShieldSpawnTime = now
        }
    }



    // =========================
    // Item Objects: Bay song song mặt đất
    // =========================
    private fun updateItemObjects() {
        val iterator = state.itemObjects.iterator()
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
            if (obj.x < -100f) {
                iterator.remove()
            }
        }
    }


    private fun maybeSpawnItemObject() {
        val now = System.currentTimeMillis()
        if (now - lastItemSpawnTime > nextItemSpawnDelay) {
            // spawn ở góc phải trên (screen coords)
            val spawnCount = Random.nextInt(0, 5) // spawn 0..5 object cùng lúc
            repeat(spawnCount) {
                val spawnX = canvasWidth + 50f + Random.nextFloat() * 10000f
                val spawnY = canvasHeight * 0.4f + Random.nextFloat() * canvasHeight * 0.2f

                state.itemObjects.add(
                    com.tona.balladventure.model.ItemObject(
                        x = spawnX,
                        y = spawnY,
//                        speedX = -(10..14).random().toFloat(), // chéo sang trái
//                        speedY = (6..8).random().toFloat()  // rơi xuống
                        speedX = -3f * obstacleSpeed,
                        speedY = 0f,
                    )
                )
            }

            lastItemSpawnTime = now
        }
    }

    // =========================
    // Collision
    // =========================
    private fun checkCollisions() {
        val player = state.player
        val playerRadiusF = playerRadius

        // --- Va chạm obstacles (AABB) ---
        val obsIterator = state.obstacles.iterator()
        while (obsIterator.hasNext()) {
            val obs = obsIterator.next()
            val obsTop = obs.y
            val obsBottom = obs.y + obs.height
            val obsLeft = obs.x
            val obsRight = obs.x + obs.width

            val playerTop = player.y - playerRadiusF
            val playerBottom = player.y + playerRadiusF
            val playerLeft = player.x - playerRadiusF
            val playerRight = player.x + playerRadiusF

            if (playerRight > obsLeft &&
                playerLeft < obsRight &&
                playerBottom > obsTop &&
                playerTop < obsBottom
            ) {
                if (!isHoldingShield) {
                    sound.playCrashSound()
                    state.isGameOver = true
                } else {
                    val hitX = (obsLeft + obsRight) / 2f
                    val hitY = (obsTop + obsBottom) / 2f
                    pendingShockwaves.offer(Pair(hitX, hitY))
                    sound.playShockwaveSound()
                    obsIterator.remove()
                }
            }
        }

        // --- Va chạm enemy (circle) ---
        val enemyIterator = state.enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val enemyCenterX = enemy.x + enemy.width / 2
            val enemyCenterY = enemy.y + enemy.height / 2
            val dx = player.x - enemyCenterX
            val dy = player.y - enemyCenterY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < playerRadiusF + enemy.size) {
                if (!isHoldingShield) {
                    sound.playCrashSound()
                    state.isGameOver = true
                }
                enemyIterator.remove()
            }
        }

        // --- Va chạm falling objects (circle) ---
        val fallIterator = state.fallingObjects.iterator()
        while (fallIterator.hasNext()) {
            val obj = fallIterator.next()
            val dx = player.x - obj.x
            val dy = player.y - obj.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < playerRadiusF + obj.size) {
                if (!isHoldingShield) {
                    sound.playCrashSound()
                    state.isGameOver = true
                }
                fallIterator.remove()
            }
        }

        // --- Va chạm item objects (circle) ---
        val itemIterator = state.itemObjects.iterator()
        while (itemIterator.hasNext()) {
            val obj = itemIterator.next()
            val dx = player.x - obj.x
            val dy = player.y - obj.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < playerRadiusF + obj.size) {
                sound.playCollectCoinSound()
                state.coin += 1
                itemIterator.remove()
            }
        }
    }


    // =========================
    // Ulti Shockwave Collision
    // =========================
    private fun checkUltiCollisions() {
        if (!isUlti) return

        val elapsed = System.currentTimeMillis() - ultiStart
        val progress = (elapsed.toFloat() / ultiDuration).coerceIn(0f, 1f)
        val maxRadius = canvasWidth.coerceAtLeast(canvasHeight) * 1.5f
        val currentRadius = maxRadius * progress

        val player = state.player
        val centerX = player.x
        val centerY = player.y

        // Enemy
        val enemyIterator = state.enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val enemyCenterX = enemy.x + enemy.width / 2
            val enemyCenterY = enemy.y + enemy.height / 2
            val dx = enemyCenterX - centerX
            val dy = enemyCenterY - centerY
            val dist = kotlin.math.sqrt(dx*dx + dy*dy)
            if (dist < currentRadius) {
                pendingShockwaves.offer(Pair(enemy.x, enemy.y))
                sound.playShockwaveSound()
                enemyIterator.remove()
            }
        }

        // Obstacles
        val obsIterator = state.obstacles.iterator()
        while (obsIterator.hasNext()) {
            val obs = obsIterator.next()
            val dx = (obs.x + obs.width / 2f) - centerX
            val dy = (obs.y + obs.height / 2f) - centerY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < currentRadius) {
                pendingShockwaves.offer(Pair(obs.x, obs.y))
                sound.playShockwaveSound()
                obsIterator.remove()
            }
        }

        // Falling objects
        val fallIterator = state.fallingObjects.iterator()
        while (fallIterator.hasNext()) {
            val obj = fallIterator.next()
            val dx = obj.x - centerX
            val dy = obj.y - centerY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < currentRadius) {
                pendingShockwaves.offer(Pair(obj.x, obj.y))
                sound.playShockwaveSound()
                fallIterator.remove()
            }
        }

        // Tắt ulti khi hết thời gian
        if (elapsed > ultiDuration) {
            isUlti = false
        }
    }

    // =========================
    // Fall trap
    // =========================
    private fun checkFallTrap() {
        if (state.gameType != Types.JUMP)
            return

        val player = state.player
        val worldX = player.x + waveOffset
        val waveY = waveAt(worldX)

        if (isInTrapZone(worldX)) {
            // Nếu chân player chạm xuống bẫy
            if (player.y + playerRadius >= waveY - 10f) {
                pendingShockwaves.offer(Pair(player.x, waveY))
                sound.playCrashSound()
                state.isGameOver = true
            }
        }
    }

    // Kiểm tra xem worldX có nằm trong phần răng cưa (đáy) của bẫy không
    private fun isInTrapZone(worldX: Float): Boolean {
        for ((start, end) in waveData.waveGaps) {
            val width = end - start
            val innerStart = start + width * 0.3f  // bỏ 30% vùng bo cong bên trái
            val innerEnd = end - width * 0.3f      // bỏ 30% vùng bo cong bên phải
            if (worldX in innerStart..innerEnd) return true
        }
        return false
    }

}
