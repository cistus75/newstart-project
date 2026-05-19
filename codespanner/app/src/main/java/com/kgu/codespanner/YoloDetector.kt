package com.kgu.codespanner // 본인의 패키지명으로 변경 필수

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class BoundingBox(
    val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float,
    val confidence: Float, val classId: Int, val className: String,
    var ocrText: String = ""
)

val KIOSK_CLASSES = arrayOf(
    "menu_back", "category_tab", "action_button",
    "option_button", "popup_modal", "cart_item"
)

class YoloDetector(private val context: Context) {

    private var interpreter: Interpreter? = null

    // YOLOv8n 기본 입력 크기 (640x640)
    private val INPUT_SIZE = 640

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        val modelBuffer = loadModelFile(context, "kiosk_yolo_int8.tflite")
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun detect(bitmap: Bitmap): ByteBuffer? {
        val tflite = interpreter ?: return null

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val outputTensor = tflite.getOutputTensor(0)
        val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
        outputBuffer.order(ByteOrder.nativeOrder())

        tflite.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return outputBuffer
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }

    fun detectAndProcess(bitmap: Bitmap): List<BoundingBox> {
        val rawOutputBuffer = detect(bitmap) ?: return emptyList()

        val outputArray = FloatArray(rawOutputBuffer.capacity() / 4)
        rawOutputBuffer.asFloatBuffer().get(outputArray)

        val numElements = 10
        val numBoxes = 8400
        val boxes = mutableListOf<BoundingBox>()

        for (i in 0 until numBoxes) {
            var maxClassProb = 0f
            var maxClassId = -1

            // TFLite 표준 [1, 10, 8400] 구조에 맞게 인덱스 파싱
            for (c in 0 until 6) {
                val prob = outputArray[(4 + c) * numBoxes + i]
                if (prob > maxClassProb) {
                    maxClassProb = prob
                    maxClassId = c
                }
            }

            // 확률 30% 이상만 통과
            if (maxClassProb > 0.3f) {
                val cx = outputArray[0 * numBoxes + i]
                val cy = outputArray[1 * numBoxes + i]
                val w = outputArray[2 * numBoxes + i]
                val h = outputArray[3 * numBoxes + i]

                // 🔥 [핵심 수정] 자동 스케일 판별기
                // cx나 너비(w)가 2.0보다 크면 픽셀(0~640)로 판단하여 640을 나누고,
                // 2.0보다 작으면 이미 비율(0~1)로 판단하여 그대로(1.0) 둡니다.
                val scale = if (cx > 2.0f || w > 2.0f) (1.0f / INPUT_SIZE) else 1.0f

                val xMin = (cx - (w / 2)) * scale
                val yMin = (cy - (h / 2)) * scale
                val xMax = (cx + (w / 2)) * scale
                val yMax = (cy + (h / 2)) * scale

                // 🔍 디버깅용 좌표 확인 로그
                android.util.Log.d("BOX_TEST", "이름: ${KIOSK_CLASSES[maxClassId]}, 비율좌표: xMin=$xMin, yMin=$yMin, w=${w*scale}")

                boxes.add(
                    BoundingBox(
                        xMin, yMin, xMax, yMax,
                        maxClassProb, maxClassId, KIOSK_CLASSES[maxClassId]
                    )
                )
            }
        }

        return applyNMS(boxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.confidence }
        val selectedBoxes = mutableListOf<BoundingBox>()

        for (box in sortedBoxes) {
            var shouldSelect = true
            for (selected in selectedBoxes) {
                if (box.classId == selected.classId && calculateIoU(box, selected) > 0.4f) {
                    shouldSelect = false
                    break
                }
            }
            if (shouldSelect) selectedBoxes.add(box)
        }
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val xA = maxOf(box1.xMin, box2.xMin)
        val yA = maxOf(box1.yMin, box2.yMin)
        val xB = minOf(box1.xMax, box2.xMax)
        val yB = minOf(box1.yMax, box2.yMax)

        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val box1Area = (box1.xMax - box1.xMin) * (box1.yMax - box1.yMin)
        val box2Area = (box2.xMax - box2.xMin) * (box2.yMax - box2.yMin)

        return interArea / (box1Area + box2Area - interArea)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}