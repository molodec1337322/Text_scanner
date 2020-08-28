package com.textscanner.app.cameraAPI

import android.graphics.Bitmap

interface OnImageCapturedHandler {
    fun onCaptured(bitmap: Bitmap)
}