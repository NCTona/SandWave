package com.tona.sandwave.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var isGameOver by mutableStateOf(false)

    fun reset() {
        player = Player(x = 240f, y = 0f, velocityY = 0f, isJumping = false)
        obstacles.clear()
        isGameOver = false
    }
}
