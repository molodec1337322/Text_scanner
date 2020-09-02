package com.textscanner.app.models

import android.util.Size

data class CameraSettings(
    val currentResolutionIndex: Int,
    val backCameraResolutionsList: MutableList<Size>,
    val displayCameraResolutionList: MutableList<String>
) {
}