package com.textscanner.app.ocr

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText

class MLKitOCRService(
    val context: Context,
    val activity: Activity,
    val onTextExtractedHandler: OnTextExtractedHandler
) {

    fun recognizeText(bitmap: Bitmap){
        val run = Runnable {
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(mutableListOf("en", "ru"))
                .build()
            val detector = FirebaseVision.getInstance().getCloudTextRecognizer(options)

            val result: Task<FirebaseVisionText> = detector.processImage(image)
                .addOnSuccessListener { result: FirebaseVisionText ->
                    activity.runOnUiThread(Runnable {
                        onTextExtractedHandler.onTextExtracted(result.text)
                    })
                }
                .addOnFailureListener { exception: Exception ->
                    activity.runOnUiThread(Runnable {
                        onTextExtractedHandler.onTextExtractionFailure(exception)
                    })
                }
        }
        Thread(run).start()
    }
}