package com.tona.sandwave.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.sandwave.model.GameState
import com.tona.sandwave.model.Obstacle
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.*

class GameEngine(val canvasWidth: Float, val canvasHeight: Float) {

    val state = GameState()
    var waveOffset by mutableStateOf(0f)
    var isGameOver by mutableStateOf(false)

    private val amplitude = 100f
    private val waveLength = 300f
    private val obstacleSpeed = 5f
    private val playerRadius = 30f
    private val gravity = 0.5f   // giảm gravity → nhảy lơ lửng lâu hơn
    private val jumpVelocity = 20f // tăng tốc ban đầu → nhảy cao hơn

    private var lastTime = System.nanoTime()

    init {
        state.reset()
    }

    fun update() {
        if (isGameOver) return

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f // giây
        lastTime = currentTime

        // Sóng trôi về phía player
        waveOffset += obstacleSpeed

        // Update player
        val player = state.player
        val baseY = canvasHeight / 2 + amplitude * sin((player.x + waveOffset)/waveLength)

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

        // Update obstacles
        state.obstacles.forEach { it.x -= obstacleSpeed }

        // Remove off-screen obstacles & spawn new
        if (state.obstacles.isNotEmpty() && state.obstacles.first().x + state.obstacles.first().width < 0) {
            state.obstacles.removeAt(0)
            state.obstacles.add(
                Obstacle(
                    x = canvasWidth + Random.nextInt(100, 300),
                    width = 40f,
                    height = 40f + Random.nextFloat() * 60f
                )
            )
        }

        // Collision detection nếu obstacles tồn tại
        if (state.obstacles.isNotEmpty()) {
            state.obstacles.forEach { obs ->
                val obsY = canvasHeight / 2 + amplitude * sin((obs.x + waveOffset)/waveLength)
                val obsTop = obsY - obs.height
                val obsBottom = obsY
                val playerTop = player.y - playerRadius
                val playerBottom = player.y + playerRadius

                if (player.x + playerRadius > obs.x && player.x - playerRadius < obs.x + obs.width &&
                    playerBottom > obsTop && playerTop < obsBottom
                ) {
                    isGameOver = true
                    state.isGameOver = true
                }
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
