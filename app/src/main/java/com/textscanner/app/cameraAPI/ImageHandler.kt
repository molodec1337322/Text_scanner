package com.textscanner.app.cameraAPI

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.Image


class ImageHandler(
    private val image: Image,
    val onImageCapturedHandler: OnImageCapturedHandler,
    val activity: Activity
) : Runnable {

    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer[bytes]
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        activity.runOnUiThread(Runnable {
            onImageCapturedHandler.onCaptured(bitmapImage)
        })
    }
}