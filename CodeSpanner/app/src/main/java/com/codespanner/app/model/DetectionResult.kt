package com.codespanner.app.model

import android.graphics.RectF

data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    val text: String = ""
)
