package com.tona.balladventure.model

data class Enemy(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val size: Float,   // bán kính cho va chạm
    val speed: Float,  // tốc độ tiến về player
    val isTop: Boolean = false  // di chuyển lên xuống
)