package com.textscanner.app

import android.media.Image

interface ImageHandler {
    fun handleImage(image: Image) :Runnable
}