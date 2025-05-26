package com.example.pawpal.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb

fun imageBitmapToAndroidBitmap(imageBitmap: ImageBitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val pixelMap = imageBitmap.toPixelMap()
    val original = Bitmap.createBitmap(
        pixelMap.width,
        pixelMap.height,
        Bitmap.Config.ARGB_8888
    )

    for (y in 0 until pixelMap.height) {
        for (x in 0 until pixelMap.width) {
            original.setPixel(x, y, pixelMap[x, y].toArgb())
        }
    }

    val ratio = minOf(
        maxWidth.toFloat() / original.width,
        maxHeight.toFloat() / original.height,
        1f // Don't upscale
    )
    val newWidth = (original.width * ratio).toInt()
    val newHeight = (original.height * ratio).toInt()

    return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
}