package com.codespanner.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.codespanner.app.model.DetectionResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class KioskDetector(context: Context) {

    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val isInputUint8: Boolean

    init {
        interpreter = Interpreter(loadModelFile(context))
        val inputShape = interpreter.getInputTensor(0).shape()
        // Expected: [1, height, width, 3]
        inputHeight = inputShape[1]
        inputWidth = inputShape[2]
        isInputUint8 = interpreter.getInputTensor(0).dataType() == DataType.UINT8
        Log.d(TAG, "Input shape: ${inputShape.toList()}, type: ${interpreter.getInputTensor(0).dataType()}")
        Log.d(TAG, "Output shape: ${interpreter.getOutputTensor(0).shape().toList()}, type: ${interpreter.getOutputTensor(0).dataType()}")
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fd = context.assets.openFd("kiosk_yolo_int8.tflite")
        val stream = FileInputStream(fd.fileDescriptor)
        return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        // Direct resize (bitmap is already cropped to view aspect by CameraActivity)
        val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

        val outputShape = interpreter.getOutputTensor(0).shape()
        val numFeatures = outputShape[1]  // 12 = 4 bbox + 8 classes
        val numBoxes = outputShape[2]     // 8400
        val output = Array(1) { Array(numFeatures) { FloatArray(numBoxes) } }

        if (isInputUint8) {
            interpreter.run(bitmapToByteBuffer(resized), output)
        } else {
            interpreter.run(bitmapToFloatArray(resized), output)
        }
        return removeContained(applyNMS(parseTransposedYoloOutput(output[0], numFeatures, numBoxes)))
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3)
        buf.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (pixel in pixels) {
            buf.put(((pixel shr 16) and 0xFF).toByte())
            buf.put(((pixel shr 8) and 0xFF).toByte())
            buf.put((pixel and 0xFF).toByte())
        }
        buf.rewind()
        return buf
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val array = Array(1) { Array(inputHeight) { Array(inputWidth) { FloatArray(3) } } }
        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = pixels[y * inputWidth + x]
                array[0][y][x][0] = ((pixel shr 16) and 0xFF) / 255f
                array[0][y][x][1] = ((pixel shr 8) and 0xFF) / 255f
                array[0][y][x][2] = (pixel and 0xFF) / 255f
            }
        }
        return array
    }

    // Output is [numFeatures, numBoxes] (transposed YOLO format)
    // Row 0-3: cx, cy, w, h normalized [0,1] → already in original image space after direct resize
    // Row 4+: class scores
    private fun parseTransposedYoloOutput(
        output: Array<FloatArray>,
        numFeatures: Int,
        numBoxes: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val confidenceThreshold = 0.20f
        val numClasses = numFeatures - 4

        for (i in 0 until numBoxes) {
            var maxScore = 0f
            for (c in 0 until numClasses) {
                val s = output[4 + c][i]
                if (s > maxScore) maxScore = s
            }
            if (maxScore < confidenceThreshold) continue

            val cx = output[0][i]
            val cy = output[1][i]
            val w  = output[2][i]
            val h  = output[3][i]

            val left   = (cx - w / 2f).coerceIn(0f, 1f)
            val top    = (cy - h / 2f).coerceIn(0f, 1f)
            val right  = (cx + w / 2f).coerceIn(0f, 1f)
            val bottom = (cy + h / 2f).coerceIn(0f, 1f)

            if (right > left && bottom > top) {
                results.add(DetectionResult(RectF(left, top, right, bottom), maxScore))
            }
        }
        return results
    }

    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }
        val kept = mutableListOf<DetectionResult>()
        for (candidate in sorted) {
            val overlaps = kept.any { iou(it.boundingBox, candidate.boundingBox) > 0.45f }
            if (!overlaps) kept.add(candidate)
        }
        return kept
    }

    // 큰 박스 안에 들어있는 작은 박스 제거
    private fun removeContained(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
        val kept = mutableListOf<DetectionResult>()
        for (candidate in sorted) {
            val isContained = kept.any { parent ->
                containmentRatio(candidate.boundingBox, parent.boundingBox) > 0.70f
            }
            if (!isContained) kept.add(candidate)
        }
        return kept
    }

    // candidate 면적 중 outer에 덮이는 비율
    private fun containmentRatio(candidate: RectF, outer: RectF): Float {
        val interLeft   = maxOf(candidate.left,   outer.left)
        val interTop    = maxOf(candidate.top,    outer.top)
        val interRight  = minOf(candidate.right,  outer.right)
        val interBottom = minOf(candidate.bottom, outer.bottom)
        if (interRight <= interLeft || interBottom <= interTop) return 0f
        val inter = (interRight - interLeft) * (interBottom - interTop)
        val area = candidate.width() * candidate.height()
        return if (area <= 0f) 0f else inter / area
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft = maxOf(a.left, b.left)
        val interTop = maxOf(a.top, b.top)
        val interRight = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        if (interRight <= interLeft || interBottom <= interTop) return 0f
        val inter = (interRight - interLeft) * (interBottom - interTop)
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0f) 0f else inter / union
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "KioskDetector"
    }
}
