package com.tona.sandwave.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var fallingObjects = mutableListOf<FallingObject>()
    var itemObjects = mutableListOf<ItemObject>()
    var shieldObjects = mutableListOf<ShieldObject>()
    val projectiles = mutableListOf<Projectile>()
    var isGameOver by mutableStateOf(false)
    var isGameWin by mutableStateOf(false)
    var isReset by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var distance: Float by mutableStateOf(0f)
    var score: Long by mutableStateOf(0)   // điểm dựa trên khoảng cách
    var coin : Int by mutableStateOf(0)
    var scale: Float by mutableStateOf(0f)

    fun reset() {
        player = Player(x = 360f, y = 0f, velocityY = 0f, isJumping = false)
        obstacles.clear()
        fallingObjects.clear()
        shieldObjects.clear()
        itemObjects.clear()
        projectiles.clear()
        isReset = false
        isGameOver = false
        isGameWin = false
    }
}
