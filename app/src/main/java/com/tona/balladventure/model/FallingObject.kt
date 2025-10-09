package com.tona.balladventure.model

data class FallingObject(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float = 35f
)
