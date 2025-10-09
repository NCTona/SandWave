package com.tona.balladventure.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tona.balladventure.level.LevelConfig
import com.tona.balladventure.level.Levels
import com.tona.balladventure.type.GameType
import com.tona.balladventure.type.Types

class GameState {
    var player = Player()
    var obstacles = mutableListOf<Obstacle>()
    var fallingObjects = mutableListOf<FallingObject>()
    var itemObjects = mutableListOf<ItemObject>()
    var shieldObjects = mutableListOf<ShieldObject>()
    val enemies = mutableListOf<Enemy>()
    var isGameOver by mutableStateOf(false)
    var isReset by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var distance: Float by mutableStateOf(0f)
    var score: Long by mutableStateOf(0)   // điểm dựa trên khoảng cách
    var coin : Int by mutableStateOf(0)
    var scale: Float by mutableStateOf(0f)

    var levelConfig by mutableStateOf(Levels.LEVEL_1)
        private set

    fun setLevel(level: LevelConfig) {
        levelConfig = level
        reset() // reset game state khi đổi level
    }

    // Kiểu chơi hiện tại
    var gameType by mutableStateOf(Types.JUMP)
        private set

    fun setType(type: GameType) {
        gameType = type
        reset()
    }

    fun reset() {
        obstacles.clear()
        fallingObjects.clear()
        shieldObjects.clear()
        itemObjects.clear()
        enemies.clear()
        isReset = false
        isGameOver = false
    }

}
