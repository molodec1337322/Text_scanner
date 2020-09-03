package com.textscanner.app.ocr

interface OnTextExtractedHandler {
    fun onTextExtracted(text: String)
    fun onTextExtractionFailure()
}