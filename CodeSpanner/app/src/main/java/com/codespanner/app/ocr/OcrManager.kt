package com.codespanner.app.ocr

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class OcrManager {

    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun extractText(bitmap: Bitmap, boundingBox: RectF, onResult: (String) -> Unit) {
        val left = (boundingBox.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (boundingBox.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (boundingBox.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (boundingBox.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val image = InputImage.fromBitmap(cropped, 0)

        recognizer.process(image)
            .addOnSuccessListener { result -> onResult(result.text.trim()) }
            .addOnFailureListener { onResult("") }
    }

    fun close() {
        recognizer.close()
    }
}
