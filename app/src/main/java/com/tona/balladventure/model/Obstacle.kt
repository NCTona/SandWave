package com.tona.balladventure.model

data class Obstacle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val isTop: Boolean = false  // di chuyển lên xuống
)
