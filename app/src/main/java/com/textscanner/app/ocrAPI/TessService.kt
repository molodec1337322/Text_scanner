package com.textscanner.app.ocrAPI

import android.graphics.Bitmap
import android.os.Environment
import com.googlecode.tesseract.android.TessBaseAPI

class TessService() {

    fun extractText(bitmap: Bitmap): String{
        val tessAPI = TessBaseAPI()
        tessAPI.init(Environment.getExternalStorageDirectory().toString() + "/Tess-two_example/", "rus")
        tessAPI.setImage(bitmap)
        val extractedText = tessAPI.utF8Text
        tessAPI.end()
        return extractedText
    }

}