package com.kgu.codespanner

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.kgu.codespanner.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // =========================================================================
    // 변수 선언부 (상태, AI 모델, UI 관련)
    // =========================================================================

    // 카메라 및 화면 렌더링
    private var imageCapture: ImageCapture? = null
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null

    // AI 분석 모델
    private lateinit var yoloDetector: YoloDetector
    private var handLandmarker: HandLandmarker? = null
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    // 데이터 저장
    private var detectedBoxes: List<BoundingBox> = emptyList()
    private var currentHoveredBox: BoundingBox? = null

    // 상호작용 (TTS 및 트래킹 상태)
    private lateinit var tts: TextToSpeech
    private var isTrackingMode = false          // 현재 라이브 트래킹 모드인지 여부
    private var hoverStartTime: Long = 0L       // 박스에 진입한 시간 기록 (체류 측정용)
    private var lastHoverTime: Long = 0L        // 손가락이 인식된 마지막 시간 (프레임 튐 방지용)
    private var isTtsSpoken: Boolean = false    // 동일한 메뉴 중복 읽기 방지 플래그

    // 디버깅 및 UI 옵션
    private var showBoundingBoxes = true        // 초록색 바운딩 박스 표시 스위치

    // 카메라 권한 요청 객체
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // 생명주기 (Lifecycle)
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TTS 및 YOLO 초기화
        initTextToSpeech()
        yoloDetector = YoloDetector(this)

        // 1. 초기 로딩 화면 실행 (모델 로드 준비)
        simulateLoadingPhase()

        // 2. 하단 캡처 바 클릭 이벤트 (상태에 따라 스캔 <-> 초기화 토글 작동)
        binding.captureBar.setOnClickListener {
            if (isTrackingMode) {
                resetToCaptureMode()
            } else {
                takePhoto()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        yoloDetector.close() // 메모리 누수 방지용 모델 반환
        tts.stop()
        tts.shutdown()
        backgroundExecutor.shutdown()
    }

    // =========================================================================
    // 초기화 및 카메라 설정
    // =========================================================================

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.KOREAN
            }
        }
    }

    private fun simulateLoadingPhase() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // 스플래시 화면 2초 유지
            binding.splashScreen.visibility = View.GONE
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 프리뷰 및 캡처 화면 비율을 16:9로 강제 고정하여 트래킹 좌표계 오차 방지
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e("CameraX", "카메라 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun resetToCaptureMode() {
        isTrackingMode = false
        binding.captureBarText.text = "여기를 눌러 화면 분석하기"
        detectedBoxes = emptyList()
        currentHoveredBox = null
        binding.overlayView.setImageBitmap(null)
        startCamera()
    }

    // =========================================================================
    // Phase 3: 정지 이미지 캡처 및 AI 분석 파이프라인 (YOLO + OCR)
    // =========================================================================

    /**
     * 1. 카메라 프레임을 캡처하여 이미지를 가져옵니다.
     * 2. 가상의 스캔 애니메이션을 띄워 사용자에게 분석 중임을 알립니다.
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        binding.captureBar.isEnabled = false // 중복 클릭 방지

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    runOnUiThread { showProcessingScreen(bitmap) }

                    // 스캔 애니메이션을 충분히 보여주기 위한 1.5초 대기 후 분석 시작
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(1500)
                        runAiAnalysisPipeline(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "사진 캡처 실패: ${exception.message}", exception)
                    binding.captureBar.isEnabled = true
                }
            }
        )
    }

    private fun showProcessingScreen(bitmap: Bitmap) {
        binding.capturedImageView.setImageBitmap(bitmap)
        binding.processingScreen.visibility = View.VISIBLE

        // 스캔 라인이 위아래로 반복해서 움직이는 애니메이션 로직
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val scanAnimator = ObjectAnimator.ofFloat(binding.scanLine, "translationY", 0f, screenHeight)
        scanAnimator.duration = 1500
        scanAnimator.repeatCount = ValueAnimator.INFINITE
        scanAnimator.repeatMode = ValueAnimator.REVERSE
        scanAnimator.start()
    }

    /**
     * YOLO로 버튼의 위치를 찾고, OCR로 텍스트를 읽어와 두 정보를 병합합니다.
     */
    private fun runAiAnalysisPipeline(bitmap: Bitmap) {
        // [1단계] YOLO를 이용해 화면 내 버튼(바운딩 박스) 추출
        detectedBoxes = yoloDetector.detectAndProcess(bitmap)
        Log.d("YOLO_TEST", "찾아낸 박스 개수: ${detectedBoxes.size}개")

        // [2단계] ML Kit OCR을 이용해 사진 속 전체 한글 텍스트 스캔
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // [3단계] OCR 텍스트 위치를 비율로 환산하여 YOLO 박스 안에 들어가는지 매칭
                for (block in visionText.textBlocks) {
                    val textBoundingBox = block.boundingBox ?: continue
                    val text = block.text.replace("\n", " ")

                    val ratioX = textBoundingBox.exactCenterX() / bitmap.width
                    val ratioY = textBoundingBox.exactCenterY() / bitmap.height

                    for (box in detectedBoxes) {
                        if (ratioX >= box.xMin && ratioX <= box.xMax && ratioY >= box.yMin && ratioY <= box.yMax) {
                            box.ocrText += "$text "
                        }
                    }
                }

                // 분석 완료 후 라이브 트래킹 모드로 전환
                transitionToLiveTracking()
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "텍스트 인식 실패", e)
                transitionToLiveTracking() // 실패하더라도 멈추지 않고 트래킹 모드로 넘어감
            }
    }

    private fun transitionToLiveTracking() {
        runOnUiThread {
            binding.processingScreen.visibility = View.GONE
            isTrackingMode = true
            binding.captureBar.isEnabled = true
            binding.captureBarText.text = "되돌아가기 (스캔 초기화)"
            startLiveTracking()
        }
    }

    // =========================================================================
    // Phase 4: 실시간 MediaPipe 트래킹 및 UI 렌더링
    // =========================================================================

    private fun startLiveTracking() {
        Toast.makeText(this, "좌표 기억 완료! 손가락을 화면에 비춰보세요.", Toast.LENGTH_SHORT).show()
        setupMediaPipe()
        startImageAnalysis()
    }

    /**
     * MediaPipe 손가락 추적기 초기화 (VIDEO 모드로 연속적인 움직임 추적)
     */
    private fun setupMediaPipe() {
        val baseOptions = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(1)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(this, options)
    }

    /**
     * 실시간 카메라 프레임을 가져오는 분석기 설정
     */
    private fun startImageAnalysis() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { imageProxy ->
                        processCameraFrame(imageProxy)
                    }
                }

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "ImageAnalysis 바인딩 실패", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 매 프레임마다 호출되어 손가락 위치를 찾고, 박스와 함께 화면에 그립니다.
     */
    private fun processCameraFrame(imageProxy: ImageProxy) {
        val originalBitmap = imageProxy.toBitmap() // 패딩 버그 자동 해결
        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        imageProxy.close()

        val screenWidth = binding.viewFinder.width
        val screenHeight = binding.viewFinder.height
        if (screenWidth <= 0 || screenHeight <= 0) return

        // 메모리 절약을 위해 도화지를 매번 새로 만들지 않고 재사용
        if (overlayBitmap == null || overlayBitmap!!.width != screenWidth || overlayBitmap!!.height != screenHeight) {
            overlayBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            overlayCanvas = Canvas(overlayBitmap!!)
        }

        // 이전 프레임의 그림 지우기
        overlayCanvas?.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        // 1. 저장해둔 바운딩 박스와 텍스트 그리기 (옵션이 켜져 있을 때만)
        if (showBoundingBoxes) {
            val boxPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = Color.GREEN }
            val textPaint = Paint().apply { color = Color.WHITE; textSize = 40f; style = Paint.Style.FILL; setShadowLayer(5f, 0f, 0f, Color.BLACK) }

            for (box in detectedBoxes) {
                val left = box.xMin * screenWidth
                val top = box.yMin * screenHeight
                val right = box.xMax * screenWidth
                val bottom = box.yMax * screenHeight

                overlayCanvas?.drawRect(left, top, right, bottom, boxPaint)
                val displayText = if (box.ocrText.isNotBlank()) box.ocrText.trim() else "(${box.className})"
                overlayCanvas?.drawText(displayText, left, top - 10, textPaint)
            }
        }

        // 2. 손가락 추적 로직 및 그리기
        if (handLandmarker != null) {
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            val frameTime = imageProxy.imageInfo.timestamp / 1_000_000 // VIDEO 모드 필수 타임스탬프
            val result = handLandmarker?.detectForVideo(mpImage, frameTime)

            if (result != null && result.landmarks().isNotEmpty()) {
                val indexFinger = result.landmarks()[0][8]
                val fingerRatioX = indexFinger.x()
                val fingerRatioY = indexFinger.y()

                val fingerPaint = Paint().apply { color = Color.RED; style = Paint.Style.FILL }
                overlayCanvas?.drawCircle(fingerRatioX * screenWidth, fingerRatioY * screenHeight, 20f, fingerPaint)

                checkIntersectionAndVibrate(fingerRatioX, fingerRatioY)
            } else {
                // 센서 노이즈로 손가락을 잃어버렸을 때 상태가 즉시 초기화되는 것을 막는 0.5초 디바운스
                if (System.currentTimeMillis() - lastHoverTime > 500) {
                    currentHoveredBox = null
                    hoverStartTime = 0L
                    isTtsSpoken = false
                }
            }
        }

        // 렌더링된 이미지를 UI에 업데이트
        runOnUiThread {
            overlayBitmap?.let {
                binding.overlayView.setImageBitmap(it)
                binding.overlayView.invalidate()
            }
        }
    }

    // =========================================================================
    // 상호작용 (햅틱 진동 및 음성 안내)
    // =========================================================================

    /**
     * 손가락이 박스 영역에 들어왔는지 판정하고, 2초 체류 시 음성을 출력합니다.
     */
    private fun checkIntersectionAndVibrate(fingerX: Float, fingerY: Float) {
        var hoveredBox: BoundingBox? = null

        for (box in detectedBoxes) {
            if (fingerX >= box.xMin && fingerX <= box.xMax && fingerY >= box.yMin && fingerY <= box.yMax) {
                hoveredBox = box
                break
            }
        }

        if (hoveredBox != null) {
            lastHoverTime = System.currentTimeMillis()

            if (currentHoveredBox != hoveredBox) {
                // 새로운 박스 진입 시 짧은 진동 피드백
                currentHoveredBox = hoveredBox
                hoverStartTime = System.currentTimeMillis()
                isTtsSpoken = false
                triggerHapticFeedback()
            } else {
                // 같은 박스에 2초 이상 체류했을 경우 TTS 재생
                val elapsedMillis = System.currentTimeMillis() - hoverStartTime
                if (elapsedMillis >= 2000 && !isTtsSpoken) {
                    speakOutBoxInfo(hoveredBox)
                    isTtsSpoken = true
                }
            }
        } else {
            // 허공으로 손가락이 나갔을 때 상태 초기화 (0.5초 딜레이)
            if (System.currentTimeMillis() - lastHoverTime > 500) {
                currentHoveredBox = null
                hoverStartTime = 0L
                isTtsSpoken = false
            }
        }
    }

    /**
     * 텍스트에서 불필요한 숫자나 기호를 걸러내고 순수 '메뉴명'만 정확하게 읽어줍니다.
     */
    private fun speakOutBoxInfo(box: BoundingBox) {
        var textToSpeak = ""

        if (box.ocrText.isNotEmpty()) {
            // 정규식을 이용해 가격 정보(숫자, 원, 콤마 등)를 삭제하여 메뉴 이름만 추출
            textToSpeak = box.ocrText.replace(Regex("[0-9원,.]"), "").trim()

            // 남은 글자가 없다면 원본 텍스트를 그대로 사용
            if (textToSpeak.isEmpty()) {
                textToSpeak = box.ocrText
            }
        } else {
            // OCR 결과가 아예 없다면 용도에 맞게 기본 번역 이름 제공
            textToSpeak = when (box.className) {
                "menu_back" -> "메뉴 영역입니다"
                "category_tab" -> "카테고리 탭입니다"
                "action_button" -> "버튼입니다"
                "option_button" -> "옵션 선택입니다"
                "popup_modal" -> "팝업 창입니다"
                "cart_item" -> "장바구니 항목입니다"
                else -> "항목입니다"
            }
        }

        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
        runOnUiThread { Toast.makeText(this, textToSpeak, Toast.LENGTH_SHORT).show() }
    }

    /**
     * 버튼 진입 시 명확한 햅틱 피드백을 제공합니다.
     */
    private fun triggerHapticFeedback() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }

    // =========================================================================
    // 헬퍼 함수
    // =========================================================================

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}