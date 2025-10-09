package com.tona.balladventure.level

data class LevelConfig(
    val obstacleSpeedFastMax: Float = 8f,
    val obstacleSpeedNormal: Float = 3.5f,
    val obstacleSpeed: Float = 3.5f,
    val gravity: Float = 0.4f,
    val gravityNormal: Float = 0.4f,
    val gravityReduced: Float = 0.15f,
    val jumpVelocity: Float = 15f,
    val pixelsPerPoint: Float = 10f, // có thể giữ mặc định
    val nextSpawnDelayRange: LongRange,
    val nextFallingDelayRange: LongRange,
    val nextItemDelayRange: LongRange,
    val nextShieldDelayRange: LongRange,
    val nextEnemyDelayRange: LongRange,
    val ultiCooldown: Long = 1500L,
    val ultiDuration: Long = 1000L,

)
