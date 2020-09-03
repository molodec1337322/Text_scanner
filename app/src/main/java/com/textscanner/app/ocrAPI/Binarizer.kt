package com.textscanner.app.ocrAPI

import android.graphics.Bitmap
import android.graphics.Color


object Binarizer {
    fun binarizeByThreshold(bitmap: Bitmap, threshold: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        for (i in 0 until size) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val luminance =
                0.299 * r + 0.0f + 0.587 * g + 0.0f + 0.114 * b + 0.0f
            pixels[i] =
                if (luminance > threshold) Color.WHITE else Color.BLACK
        }
        val binarizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        binarizedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return binarizedBitmap
    }
}