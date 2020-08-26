package com.textscanner.app

import android.graphics.Bitmap

interface OnImageCapturedHandler {
    fun onCaptured(bitmap: Bitmap)
}