package com.tona.sandwave.model

import kotlin.random.Random

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var isGameOver = false

    fun reset() {
        player = Player(x = 120f, y = 0f, velocityY = 0f, isJumping = false)
        obstacles.clear()
        isGameOver = false
    }
}
