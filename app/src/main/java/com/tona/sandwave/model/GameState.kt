package com.tona.sandwave.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var isGameOver by mutableStateOf(false)
    var score: Long by mutableStateOf(0)   // điểm dựa trên khoảng cách

    fun reset() {
        player = Player(x = 360f, y = 0f, velocityY = 0f, isJumping = false)
        obstacles.clear()
        isGameOver = false
    }
}
