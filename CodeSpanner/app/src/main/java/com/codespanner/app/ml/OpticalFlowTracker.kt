package com.codespanner.app.ml

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video

class OpticalFlowTracker {

    private var prevGray: Mat? = null
    private var prevPoints: MatOfPoint2f? = null
    private var trackW = 0
    private var trackH = 0
    private var needsInit = true

    companion object {
        private const val SCALE_WIDTH = 480
        private const val MAX_FEATURES = 300
        private const val MIN_FEATURES = 20
        private const val QUALITY = 0.01
        private const val MIN_DIST = 8.0
        private const val THRESHOLD = 0.003f  // ignore sub-pixel noise
        private const val MAX_DELTA = 0.05f   // clamp wild jumps
    }

    // Called when entering interaction mode — actual init happens on first analysis frame
    fun prepareForTracking() {
        needsInit = true
        reset()
    }

    // Returns normalized (dx, dy) to apply to bounding boxes, or null if not ready
    fun track(bitmap: Bitmap): Pair<Float, Float>? {
        // Initialize from the first analysis frame so reference and tracking use same stream
        if (needsInit) {
            initialize(bitmap)
            needsInit = false
            return null
        }

        val prev = prevGray ?: return null
        val pts = prevPoints ?: return null

        if (pts.rows() < MIN_FEATURES) {
            initialize(bitmap)
            return null
        }

        val currGray = toGray(bitmap)
        val nextPts = MatOfPoint2f()
        val status = MatOfByte()
        val err = MatOfFloat()

        Video.calcOpticalFlowPyrLK(prev, currGray, pts, nextPts, status, err)

        val statusArr = status.toArray()
        val prevArr = pts.toArray()
        val nextArr = nextPts.toArray()

        val dxList = ArrayList<Float>(statusArr.size)
        val dyList = ArrayList<Float>(statusArr.size)
        val goodPts = ArrayList<Point>(statusArr.size)

        for (i in statusArr.indices) {
            if (statusArr[i].toInt() == 1) {
                dxList.add((nextArr[i].x - prevArr[i].x).toFloat())
                dyList.add((nextArr[i].y - prevArr[i].y).toFloat())
                goodPts.add(nextArr[i])
            }
        }

        prevGray?.release()
        prevGray = currGray
        prevPoints = MatOfPoint2f(*goodPts.toTypedArray())

        if (dxList.size < MIN_FEATURES) return null

        val dx = (median(dxList) / trackW).coerceIn(-MAX_DELTA, MAX_DELTA)
        val dy = (median(dyList) / trackH).coerceIn(-MAX_DELTA, MAX_DELTA)

        // Dead zone: ignore tiny vibrations
        if (Math.abs(dx) < THRESHOLD && Math.abs(dy) < THRESHOLD) return null

        return Pair(dx, dy)
    }

    fun reset() {
        prevGray?.release(); prevGray = null
        prevPoints?.release(); prevPoints = null
    }

    private fun initialize(bitmap: Bitmap) {
        reset()
        val scale = SCALE_WIDTH.toFloat() / bitmap.width
        trackW = SCALE_WIDTH
        trackH = (bitmap.height * scale).toInt()
        val gray = toGray(bitmap)
        val corners = MatOfPoint()
        Imgproc.goodFeaturesToTrack(gray, corners, MAX_FEATURES, QUALITY, MIN_DIST)
        prevGray = gray
        prevPoints = MatOfPoint2f(*corners.toArray())
    }

    private fun median(list: ArrayList<Float>): Float {
        list.sort()
        return list[list.size / 2]
    }

    private fun toGray(bitmap: Bitmap): Mat {
        val scaled = Bitmap.createScaledBitmap(bitmap, trackW, trackH, false)
        val rgba = Mat()
        Utils.bitmapToMat(scaled, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        return gray
    }
}
