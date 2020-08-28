package com.textscanner.app

import android.util.Size

data class CameraSettings(
    val currentResolutionIndex: Int,
    val backCameraResolutionsList: MutableList<Size>,
    val displayCameraResolutionList: MutableList<String>
) {
}