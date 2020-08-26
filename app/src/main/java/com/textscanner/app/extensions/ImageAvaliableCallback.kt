package com.textscanner.app.extensions

import android.media.Image

interface ImageAvailableCallback {
    fun getBitmapImage(image: Image): Runnable
}