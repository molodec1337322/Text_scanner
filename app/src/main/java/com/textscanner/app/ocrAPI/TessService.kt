package com.textscanner.app.ocrAPI

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.textscanner.app.App


class TessService(
    val activity: Activity,
    val language: String,
    val onTextExtracted: OnTextExtracted
) {

    fun extractText(bitmap: Bitmap){
        val run = Runnable {
            val binarizedBitmap = Binarizer.binarizeByThreshold(bitmap, 128)
            bitmap.recycle()

            val tessAPI = TessBaseAPI()
            val path = App.instance!!.tessDataParentDirectory
            tessAPI.init(path, "$language")
            tessAPI.setImage(binarizedBitmap)
            val extractedText = tessAPI.utF8Text
            tessAPI.end()
            activity.runOnUiThread(Runnable {
                onTextExtracted.onTextExtracted(extractedText)
            })
        }
        Thread(run).start()
    }
}