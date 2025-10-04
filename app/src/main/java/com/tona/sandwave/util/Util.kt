package com.tona.sandwave.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntRect
import androidx.core.content.ContextCompat
import com.tona.sandwave.R

@Composable
fun rememberShieldBitmap(): ImageBitmap {
    val context = LocalContext.current
    val drawable = ContextCompat.getDrawable(context, R.drawable.baseline_gpp_good_24)!!
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}


/**
 * Tách sprite sheet thành list các frame ImageBitmap
 *
 * @param spriteSheet Bitmap gốc (đã load từ PNG)
 * @param columns số cột trong sheet
 * @param rows số hàng trong sheet
 * @return list các frame dạng ImageBitmap
 */
fun splitSpriteSheet(spriteSheet: Bitmap, columns: Int, rows: Int): List<ImageBitmap> {
    val frameWidth = spriteSheet.width / columns
    val frameHeight = spriteSheet.height / rows
    val frames = mutableListOf<ImageBitmap>()

    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val frame = Bitmap.createBitmap(
                spriteSheet,
                x * frameWidth,
                y * frameHeight,
                frameWidth,
                frameHeight
            )
            frames.add(frame.asImageBitmap())
        }
    }
    return frames
}


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

fun removeBlueBackground(bitmap: Bitmap): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val targetColor = Color(0xFF040E24.toInt()).toArgb() // màu nền
    val tolerance = 40 // độ chênh lệch cho phép

    for (y in 0 until result.height) {
        for (x in 0 until result.width) {
            val pixel = result.getPixel(x, y)

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val tr = (targetColor shr 16) and 0xFF
            val tg = (targetColor shr 8) and 0xFF
            val tb = targetColor and 0xFF

            // Nếu màu pixel gần giống màu nền → làm trong suốt
            if (Math.abs(r - tr) < tolerance &&
                Math.abs(g - tg) < tolerance &&
                Math.abs(b - tb) < tolerance
            ) {
                result.setPixel(x, y, android.graphics.Color.TRANSPARENT)
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

fun lerpColor(from: Color, to: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = (1 - tt) * from.red + tt * to.red,
        green = (1 - tt) * from.green + tt * to.green,
        blue = (1 - tt) * from.blue + tt * to.blue,
        alpha = (1 - tt) * from.alpha + tt * to.alpha  // sử dụng alpha của color gốc
    )
}
