package com.tona.sandwave.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.sandwave.model.GameState
import com.tona.sandwave.model.Obstacle
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(val canvasWidth: Float, val canvasHeight: Float) {

    val state = GameState()
    var waveOffset by mutableStateOf(0f)
    var isGameOver by mutableStateOf(false)

    private val amplitude = 100f
    private val waveLength = 300f
    private val obstacleSpeed = 3.5f
    private val playerRadius = 30f
    private val gravity = 0.4f
    private val jumpVelocity = 15f

    private var lastTime = System.nanoTime()

    // spawn control
    private var lastSpawnTime = System.currentTimeMillis()
    private var nextSpawnDelay = Random.nextLong(3000, 5000) // ms

    init {
        state.reset()
    }

    fun update() {
        if (isGameOver) return

        // deltaTime để vật lý mượt hơn
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
        lastTime = currentTime

        // cập nhật sóng
        waveOffset += obstacleSpeed

        updatePlayer(deltaTime)
        updateObstacles()
        maybeSpawnObstacle()
        checkCollisions()
    }

    private fun updatePlayer(deltaTime: Float) {
        val player = state.player
        val baseY = canvasHeight / 2 + amplitude * sin((player.x + waveOffset) / waveLength)

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

    private fun updateObstacles() {
        // di chuyển sang trái
        state.obstacles.forEach { it.x -= obstacleSpeed }
        // xoá những obstacle đã ra ngoài
        state.obstacles.removeAll { it.x + it.width < 0 }
    }

    private fun maybeSpawnObstacle() {
        val now = System.currentTimeMillis()
        if (now - lastSpawnTime > nextSpawnDelay) {
            state.obstacles.add(
                Obstacle(
                    x = canvasWidth + Random.nextInt(5000, 10000),
                    width = 40f,
                    height = 40f + Random.nextFloat() * 60f
                )
            )
            lastSpawnTime = now
            nextSpawnDelay = Random.nextLong(3000, 5000) // random lại
        }
    }

    private fun checkCollisions() {
        val player = state.player
        val playerTop = player.y - playerRadius
        val playerBottom = player.y + playerRadius

        state.obstacles.forEach { obs ->
            val obsY = canvasHeight / 2 + amplitude * sin((obs.x + waveOffset) / waveLength)
            val obsTop = obsY - obs.height
            val obsBottom = obsY

            if (player.x + playerRadius > obs.x &&
                player.x - playerRadius < obs.x + obs.width &&
                playerBottom > obsTop && playerTop < obsBottom
            ) {
                isGameOver = true
                state.isGameOver = true
            }
        }
    }

    fun playerJump() {
        if (!state.player.isJumping) {
            state.player.velocityY = jumpVelocity
            state.player.isJumping = true
        }
    }
}
