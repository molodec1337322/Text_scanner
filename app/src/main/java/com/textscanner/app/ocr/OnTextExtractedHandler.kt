package com.textscanner.app.ocr

import java.lang.Exception

interface OnTextExtractedHandler {
    fun onTextExtracted(text: String)
    fun onTextExtractionFailure(exception: Exception)
}