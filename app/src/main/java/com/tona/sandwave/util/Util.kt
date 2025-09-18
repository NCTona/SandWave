package com.tona.sandwave.util

import android.graphics.Bitmap

fun removeWhiteBackground(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = bitmap.getPixel(x, y)
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            if (r > 240 && g > 240 && b > 240) {
                result.setPixel(x, y, 0x00000000) // trong suốt
            } else {
                result.setPixel(x, y, color or 0xFF000000.toInt())
            }
        }
    }
    return result
}

fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
    val scaled = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(scaled)
    val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
    val dstRect = android.graphics.Rect(0, 0, width, height)
    canvas.drawBitmap(bitmap, srcRect, dstRect, null)
    return scaled
}
