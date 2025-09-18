package com.tona.sandwave.animation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

@Composable
fun SpriteAnimation(
    spriteSheet: ImageBitmap,
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier,
    frameDuration: Long = 120L,
    x: Float,
    y: Float,
    size: Float
) {
    val totalFrames = rows * cols
    var currentFrame by remember { mutableStateOf(0) }

    // vòng lặp đổi frame
    LaunchedEffect(Unit) {
        while (true) {
            delay(frameDuration)
            currentFrame = (currentFrame + 1) % totalFrames
        }
    }

    val frameWidth = spriteSheet.width / cols
    val frameHeight = spriteSheet.height / rows

    Canvas(modifier = modifier) {
        val row = currentFrame / cols
        val col = currentFrame % cols

        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(col * frameWidth, row * frameHeight),
            srcSize = IntSize(frameWidth, frameHeight),
            dstOffset = IntOffset((x - size / 2).toInt(), (y - size / 2).toInt()),
            dstSize = IntSize(size.toInt(), size.toInt())
        )
    }
}
