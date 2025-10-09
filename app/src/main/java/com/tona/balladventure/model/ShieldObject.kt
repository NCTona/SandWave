package com.tona.balladventure.model

data class ShieldObject(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float = 35f,
    val isTop: Boolean = false,
)
