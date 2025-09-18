package com.tona.sandwave.model

data class Projectile(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val radius: Float = 20f
)