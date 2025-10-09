package com.tona.balladventure.model

data class Player(
    var x: Float = 360f,
    var y: Float = 0f,
    var velocityY: Float = 0f,      // tốc độ rơi / bay
    var isJumping: Boolean = false,  // đang nhảy hay không
)
