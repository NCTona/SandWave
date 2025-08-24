package com.tona.sandwave.model

data class Player(
    var x: Float = 120f,
    var y: Float = 0f,
    var velocityY: Float = 0f,
    var isJumping: Boolean = false  // thêm dòng này
)
