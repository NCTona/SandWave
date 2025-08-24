package com.tona.sandwave.model

import kotlin.random.Random

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var isGameOver = false


    fun spawnObstacles() {
        val rng = Random
        obstacles.add(
            Obstacle(
                x = 500f,
                width = 40f,
                height = 40f + rng.nextFloat() * 60f
            )
        )
    }

    fun reset() {
        player = Player(x = 120f, y = 0f, velocityY = 0f, isJumping = false)
        obstacles.clear()
        isGameOver = false
        spawnObstacles()
    }

}
