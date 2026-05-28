package com.codespanner.app.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.codespanner.app.model.DetectionResult

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private val ACCENT = Color.parseColor("#C8FF00")
        private const val CORNER_LEN = 72f
        private const val CORNER_STROKE = 5f
    }

    // Detection box paints
    private val boxPaint = Paint().apply {
        color = ACCENT; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true
    }
    private val activeBoxPaint = Paint().apply {
        color = ACCENT; style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true
    }
    private val progressPaint = Paint().apply {
        color = ACCENT; style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true
    }

    // Viewfinder corner paint
    private val cornerPaint = Paint().apply {
        color = ACCENT
        style = Paint.Style.STROKE
        strokeWidth = CORNER_STROKE
        strokeCap = Paint.Cap.SQUARE
        isAntiAlias = true
    }

    // Finger dot
    private val dotFillPaint = Paint().apply {
        color = ACCENT; style = Paint.Style.FILL; isAntiAlias = true
    }
    private val dotRingPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
    }

    // Text label
    private val textBgPaint = Paint().apply {
        color = Color.parseColor("#CC000000"); style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }

    // Scan animation — glow drawn as layered lines (hardware-compatible)
    private val scanGlowPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    private val circlePaint = Paint().apply {
        color = ACCENT; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
    }
    private val innerDotPaint = Paint().apply {
        color = ACCENT; style = Paint.Style.FILL; isAntiAlias = true
    }

    private var detections = listOf<DetectionResult>()
    private var fingerPoint: PointF? = null
    private var dwellingIndex = -1
    private var dwellProgress = 0f
    private var isScanning = false
    private var scanLineY = 0.35f

    private val scanAnimator = ValueAnimator.ofFloat(0.25f, 0.75f).apply {
        duration = 1800
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { scanLineY = it.animatedValue as Float; invalidate() }
    }

    fun setDetections(results: List<DetectionResult>) { detections = results; invalidate() }
    fun setFingerPoint(point: PointF?) { fingerPoint = point; invalidate() }
    fun setDwellState(index: Int, progress: Float) { dwellingIndex = index; dwellProgress = progress; invalidate() }

    fun setScanning(scanning: Boolean) {
        isScanning = scanning
        if (scanning) { if (!scanAnimator.isRunning) scanAnimator.start() }
        else scanAnimator.cancel()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCorners(canvas)
        if (isScanning) drawScanAnimation(canvas)
        else { drawDetections(canvas); drawFinger(canvas) }
    }

    private fun drawCorners(canvas: Canvas) {
        val ml = width * 0.10f
        val mr = width * 0.90f
        val mt = height * 0.08f
        val mb = height * 0.88f
        val l = CORNER_LEN

        // Top-left
        canvas.drawLine(ml, mt + l, ml, mt, cornerPaint)
        canvas.drawLine(ml, mt, ml + l, mt, cornerPaint)
        // Top-right
        canvas.drawLine(mr - l, mt, mr, mt, cornerPaint)
        canvas.drawLine(mr, mt, mr, mt + l, cornerPaint)
        // Bottom-left
        canvas.drawLine(ml, mb - l, ml, mb, cornerPaint)
        canvas.drawLine(ml, mb, ml + l, mb, cornerPaint)
        // Bottom-right
        canvas.drawLine(mr - l, mb, mr, mb, cornerPaint)
        canvas.drawLine(mr, mb, mr, mb - l, cornerPaint)
    }

    private fun drawScanAnimation(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // Concentric circles (fading outward)
        val radii = floatArrayOf(20f, 44f, 68f)
        val alphas = intArrayOf(255, 160, 80)
        for (i in radii.indices) {
            circlePaint.alpha = alphas[i]
            canvas.drawCircle(cx, cy, radii[i], circlePaint)
        }
        canvas.drawCircle(cx, cy, 7f, innerDotPaint)

        // Glow scan line (multiple layers)
        val lineY = scanLineY * height
        val lineL = width * 0.10f
        val lineR = width * 0.90f
        val glowLayers = arrayOf(
            Pair(28f, 30),
            Pair(14f, 60),
            Pair(6f, 120),
            Pair(2f, 255)
        )
        for ((stroke, alpha) in glowLayers) {
            scanGlowPaint.strokeWidth = stroke
            scanGlowPaint.color = Color.argb(alpha, 0xC8, 0xFF, 0x00)
            canvas.drawLine(lineL, lineY, lineR, lineY, scanGlowPaint)
        }
    }

    private fun drawDetections(canvas: Canvas) {
        detections.forEachIndexed { index, det ->
            val rect = RectF(
                det.boundingBox.left * width,
                det.boundingBox.top * height,
                det.boundingBox.right * width,
                det.boundingBox.bottom * height
            )
            canvas.drawRect(rect, if (index == dwellingIndex) activeBoxPaint else boxPaint)

            if (index == dwellingIndex) {
                val r = 24f
                val arc = RectF(rect.left + 4f, rect.top + 4f, rect.left + 4f + r * 2, rect.top + 4f + r * 2)
                canvas.drawArc(arc, -90f, 360f * dwellProgress, false, progressPaint)
            }

            if (det.text.isNotEmpty()) {
                val label = if (det.text.length > 16) det.text.take(16) + "…" else det.text
                val tw = textPaint.measureText(label)
                canvas.drawRoundRect(RectF(rect.left, rect.top - 34f, rect.left + tw + 8f, rect.top), 4f, 4f, textBgPaint)
                canvas.drawText(label, rect.left + 4f, rect.top - 8f, textPaint)
            }
        }
    }

    private fun drawFinger(canvas: Canvas) {
        fingerPoint?.let {
            val x = it.x * width; val y = it.y * height
            canvas.drawCircle(x, y, 16f, dotFillPaint)
            canvas.drawCircle(x, y, 16f, dotRingPaint)
        }
    }
}
