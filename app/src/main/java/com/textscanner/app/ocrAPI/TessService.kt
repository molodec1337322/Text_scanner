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

            val binarizedBitmap = Binarizer.binarizeByThreshold(bitmap, 80)
            bitmap.recycle()

            val tessAPI = TessBaseAPI()
            val path = App.instance!!.tessDataParentDirectory
            tessAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя1234567890,.?! ")

            tessAPI.setVariable("load_system_dawg", "0")
            tessAPI.setVariable("load_freq_dawg", "0")
            tessAPI.setVariable("load_unambig_dawg", "0")
            tessAPI.setVariable("load_punc_dawg", "0")
            tessAPI.setVariable("load_number_dawg", "0")
            tessAPI.setVariable("load_fixed_length_dawgs", "0")
            tessAPI.setVariable("load_bigram_dawg", "0")
            tessAPI.setVariable("wordrec_enable_assoc", "0")
            tessAPI.setVariable("tessedit_enable_bigram_correction", "0")
            tessAPI.setVariable("assume_fixed_pitch_char_segment", "1")

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