package com.tona.balladventure.level

object Levels {
    val LEVEL_1 = LevelConfig(
        nextSpawnDelayRange = 2500L..3500L,
        nextFallingDelayRange = 3000L..4000L,
        nextItemDelayRange = 2000L..6000L,
        nextShieldDelayRange = 5000L..10000L,
        nextEnemyDelayRange = 5000L..7000L
    )

    val LEVEL_2 = LEVEL_1.copy(
        nextSpawnDelayRange = 1500L..2500L,
        nextFallingDelayRange = 2000L..3000L,
        nextItemDelayRange = 3000L..7000L,
        nextShieldDelayRange = 6000L..11000L,
        nextEnemyDelayRange = 4000L..6000L
    )

    val LEVEL_3 = LEVEL_1.copy(
        nextSpawnDelayRange = 500L..1500L,
        nextFallingDelayRange = 1000L..2000L,
        nextItemDelayRange = 4000L..8000L,
        nextShieldDelayRange = 7000L..12000L,
        nextEnemyDelayRange = 2000L..4000L
    )
}

