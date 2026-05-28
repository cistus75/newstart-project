package com.codespanner.app.ml

import android.content.Context
import android.graphics.PointF
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class FingerTracker(
    context: Context,
    private val onResult: (PointF?) -> Unit
) {
    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setResultListener { result, _ -> processResult(result) }
            .setErrorListener { error -> Log.e(TAG, "MediaPipe error: $error") }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(image: MPImage, timestampMs: Long) {
        handLandmarker.detectAsync(image, timestampMs)
    }

    private fun processResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) {
            onResult(null)
            return
        }
        // Landmark index 8 = index finger tip
        val tip = result.landmarks()[0][8]
        onResult(PointF(tip.x(), tip.y()))
    }

    fun close() {
        handLandmarker.close()
    }

    companion object {
        private const val TAG = "FingerTracker"
    }
}
