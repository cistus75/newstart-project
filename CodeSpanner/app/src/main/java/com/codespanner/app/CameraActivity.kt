package com.codespanner.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.codespanner.app.ml.FingerTracker
import com.codespanner.app.ml.KioskDetector
import com.codespanner.app.ml.OpticalFlowTracker
import com.codespanner.app.model.DetectionResult
import com.codespanner.app.ocr.OcrManager
import com.codespanner.app.tts.TtsManager
import com.codespanner.app.view.OverlayView
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.*
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {

    private enum class AppState { SCANNING, PROCESSING, INTERACTION }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvStatus: TextView
    private lateinit var btnCapture: View
    private lateinit var btnHome: android.widget.Button
    private lateinit var controlBar: LinearLayout

    private lateinit var kioskDetector: KioskDetector
    private lateinit var fingerTracker: FingerTracker
    private lateinit var ocrManager: OcrManager
    private lateinit var ttsManager: TtsManager
    private val opticalFlowTracker = OpticalFlowTracker()

    private var currentState = AppState.SCANNING
    @Volatile private var storedDetections = listOf<DetectionResult>()
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var viewW = 0
    @Volatile private var viewH = 0

    private var dwellStartTime = 0L
    private var dwellIndex = -1
    private var lastSpokenIndex = -1
    private val dwellDurationMs = 1000L

    private var flowFrameCounter = 0

    // EMA smoothing for finger coordinates
    private val emaAlpha = 0.25f
    private var emaX = 0f
    private var emaY = 0f
    private var emaInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (!OpenCVLoader.initLocal()) Log.e("OpenCV", "init failed")

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvStatus   = findViewById(R.id.tvStatus)
        btnCapture = findViewById(R.id.btnCapture)
        btnHome    = findViewById(R.id.btnHome)
        controlBar = findViewById(R.id.controlBar)

        // 네비게이션 바(홈·뒤로가기) 높이만큼 하단 패딩 적용
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(controlBar) { v, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(0, 0, 0, navBottom)
            insets
        }

        overlayView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                overlayView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewW = overlayView.width
                viewH = overlayView.height
            }
        })

        kioskDetector = KioskDetector(this)
        ocrManager = OcrManager()
        ttsManager = TtsManager(this)
        fingerTracker = FingerTracker(this) { fingerPoint ->
            runOnUiThread { handleFingerPoint(fingerPoint) }
        }

        btnCapture.setOnClickListener {
            when (currentState) {
                AppState.SCANNING -> captureAndProcess()
                AppState.PROCESSING -> Unit
                AppState.INTERACTION -> Unit
            }
        }
        btnHome.setOnClickListener { resetToScanMode() }

applyState(AppState.SCANNING)

        if (checkPermission()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }

    private fun applyState(state: AppState) {
        currentState = state
        when (state) {
            AppState.SCANNING -> {
                tvStatus.text = "키오스크 화면을 비춰주세요."
                tvStatus.visibility = android.view.View.VISIBLE
                tvStatus.alpha = 1f
                btnCapture.visibility = android.view.View.VISIBLE
                btnCapture.isEnabled = true
                btnCapture.alpha = 1f
                btnHome.visibility = android.view.View.GONE
                overlayView.setScanning(false)
                overlayView.setDetections(emptyList())
                overlayView.setFingerPoint(null)
            }
            AppState.PROCESSING -> {
                tvStatus.text = "키오스크 인식 중..."
                tvStatus.visibility = android.view.View.VISIBLE
                tvStatus.alpha = 0.7f
                btnCapture.visibility = android.view.View.VISIBLE
                btnCapture.isEnabled = false
                btnCapture.alpha = 0.4f
                btnHome.visibility = android.view.View.GONE
                overlayView.setScanning(true)
            }
            AppState.INTERACTION -> {
                tvStatus.visibility = android.view.View.GONE
                btnCapture.visibility = android.view.View.GONE
                btnHome.visibility = android.view.View.VISIBLE
                overlayView.setScanning(false)
            }
        }
    }

    private fun checkPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            bindCamera(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            if (currentState == AppState.INTERACTION) processFrameForFinger(imageProxy)
            else imageProxy.close()
        }

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            this, CameraSelector.DEFAULT_BACK_CAMERA,
            preview, imageCapture, imageAnalysis
        )
    }

    private fun captureAndProcess() {
        val capture = imageCapture ?: return
        applyState(AppState.PROCESSING)

        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val action = FocusMeteringAction.Builder(factory.createPoint(0.5f, 0.5f))
            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)

        lifecycleScope.launch {
            delay(800)
            capture.takePicture(ContextCompat.getMainExecutor(this@CameraActivity), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toRotatedBitmap().cropToViewAspect()
                    image.close()
                    lifecycleScope.launch { processKiosk(bitmap) }
                }
                override fun onError(exception: ImageCaptureException) {
                    applyState(AppState.SCANNING)
                    Toast.makeText(this@CameraActivity, "촬영 실패", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private suspend fun processKiosk(bitmap: Bitmap) {
        val detections = withContext(Dispatchers.Default) { kioskDetector.detect(bitmap) }

        val results = mutableListOf<DetectionResult>()
        for (detection in detections) {
            val deferred = CompletableDeferred<String>()
            withContext(Dispatchers.Main) {
                ocrManager.extractText(bitmap, detection.boundingBox) { deferred.complete(it) }
            }
            results.add(detection.copy(text = parseMenuText(deferred.await())))
        }

        storedDetections = results

        withContext(Dispatchers.Main) {
            if (results.isEmpty()) {
                applyState(AppState.SCANNING)
                Toast.makeText(this@CameraActivity, "키오스크를 찾지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                ttsManager.speak("키오스크를 찾지 못했습니다. 다시 시도해 주세요.")
            } else {
                enterInteractionMode()
            }
        }
    }

    private fun enterInteractionMode() {
        applyState(AppState.INTERACTION)
        overlayView.setDetections(storedDetections)
        ttsManager.speak("키오스크 분석 완료. 검지손가락으로 원하는 항목을 가리켜 주세요.")
        opticalFlowTracker.prepareForTracking()
        flowFrameCounter = 0
    }

    private fun processFrameForFinger(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toRotatedBitmap().cropToViewAspect()
        imageProxy.close()

        if (flowFrameCounter++ % 2 == 0) {
            val delta = opticalFlowTracker.track(bitmap)
            if (delta != null) {
                val (dx, dy) = delta
                val updated = storedDetections.map { det ->
                    det.copy(boundingBox = RectF(
                        (det.boundingBox.left  + dx).coerceIn(0f, 1f),
                        (det.boundingBox.top   + dy).coerceIn(0f, 1f),
                        (det.boundingBox.right + dx).coerceIn(0f, 1f),
                        (det.boundingBox.bottom + dy).coerceIn(0f, 1f)
                    ))
                }
                storedDetections = updated
                runOnUiThread { overlayView.setDetections(updated) }
            }
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        fingerTracker.detect(mpImage, System.currentTimeMillis())
    }

    private fun handleFingerPoint(rawPoint: PointF?) {
        if (rawPoint == null) {
            emaInitialized = false
            overlayView.setFingerPoint(null)
            resetDwell(); lastSpokenIndex = -1; return
        }

        // EMA smoothing
        if (!emaInitialized) {
            emaX = rawPoint.x; emaY = rawPoint.y; emaInitialized = true
        } else {
            emaX = emaAlpha * rawPoint.x + (1f - emaAlpha) * emaX
            emaY = emaAlpha * rawPoint.y + (1f - emaAlpha) * emaY
        }
        val point = PointF(emaX, emaY)

        overlayView.setFingerPoint(point)

        val hitIndex = storedDetections.indexOfFirst { it.boundingBox.contains(point.x, point.y) }
        if (hitIndex == -1) { resetDwell(); lastSpokenIndex = -1; return }

        if (hitIndex == lastSpokenIndex) return

        if (hitIndex != dwellIndex) {
            dwellIndex = hitIndex
            dwellStartTime = System.currentTimeMillis()
        }

        val elapsed = System.currentTimeMillis() - dwellStartTime
        overlayView.setDwellState(dwellIndex, (elapsed / dwellDurationMs.toFloat()).coerceIn(0f, 1f))

        if (elapsed >= dwellDurationMs) {
            val text = storedDetections[hitIndex].text
            triggerHaptic()
            ttsManager.speak(if (text.isNotEmpty()) text else "텍스트 없음")
            lastSpokenIndex = hitIndex
            resetDwell()
        }
    }

    private fun triggerHaptic() {
        val effect = VibrationEffect.createOneShot(250, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
    }

    private fun resetDwell() {
        dwellIndex = -1; dwellStartTime = 0L
        overlayView.setDwellState(-1, 0f)
    }

    private fun resetToScanMode() {
        opticalFlowTracker.reset()
        storedDetections = emptyList()
        resetDwell()
        lastSpokenIndex = -1
        ttsManager.stop()
        applyState(AppState.SCANNING)
    }

    private fun parseMenuText(raw: String): String {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val priceRegex = Regex("""[\d,]+원""")

        val excluded = setOf(
            "신메뉴", "new", "best", "추천", "인기", "md추천", "md 추천",
            "커피", "에이드", "프라페", "스무디", "논커피", "디저트", "추천메뉴", "프라페/스무디"
        )
        val name = lines.firstOrNull { line ->
            !priceRegex.containsMatchIn(line) &&
            line.trim().lowercase() !in excluded &&
            line.any { it.isLetter() }
        }

        // DB 매칭 성공 시 정확한 이름 + 가격 사용 (OCR 오타 보정)
        if (name != null) {
            val matched = com.codespanner.app.ml.MenuMatcher.match(name)
            if (matched != null) return "${matched.name} ${matched.price}원"
        }

        // fallback: OCR 텍스트 그대로 사용
        val price = lines.firstOrNull { priceRegex.containsMatchIn(it) }
            ?.let { priceRegex.find(it)?.value?.replace(",", "") }

        return when {
            name != null && price != null -> "$name $price"
            name != null -> name
            price != null -> price
            else -> lines.firstOrNull() ?: ""
        }
    }

    private fun Bitmap.cropToViewAspect(): Bitmap {
        val w = viewW.takeIf { it > 0 } ?: return this
        val h = viewH.takeIf { it > 0 } ?: return this
        val targetAspect = w.toFloat() / h
        val srcAspect = width.toFloat() / height
        return when {
            srcAspect > targetAspect -> {
                val newW = (height * targetAspect).toInt()
                Bitmap.createBitmap(this, (width - newW) / 2, 0, newW, height)
            }
            srcAspect < targetAspect -> {
                val newH = (width / targetAspect).toInt()
                Bitmap.createBitmap(this, 0, (height - newH) / 2, width, newH)
            }
            else -> this
        }
    }

    private fun ImageProxy.toRotatedBitmap(): Bitmap {
        val bitmap = this.toBitmap()
        val rotation = this.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        opticalFlowTracker.reset()
        kioskDetector.close()
        fingerTracker.close()
        ocrManager.close()
        ttsManager.shutdown()
        analysisExecutor.shutdown()
    }
}
