# 📂 CodeSpanner 코드 아키텍처 및 모듈 명세

## 1. 패키지 구조 (Package Tree)

```text
com.codespanner.app
├── SplashActivity.kt          # 앱 진입 스플래시 화면
├── CameraActivity.kt          # 메인 컨트롤러 및 상태 관리자
├── ml/                        # 온디바이스 AI 비전 모듈
│   ├── KioskDetector.kt       # YOLOv8 기반 키오스크 버튼 탐지
│   ├── FingerTracker.kt       # MediaPipe 기반 검지 손가락 추적
│   ├── OpticalFlowTracker.kt  # OpenCV 기반 카메라 흔들림 보정
│   └── MenuMatcher.kt         # Levenshtein 거리 기반 오타 보정 DB 매칭
├── ocr/                       # 텍스트 오프라인 인식 모듈
│   └── OcrManager.kt          # ML Kit 기반 영역 크롭 및 문자 추출
├── tts/                       # 음성 안내 모듈
│   └── TtsManager.kt          # Android TextToSpeech 래퍼
├── view/                      # UI 및 커스텀 그래픽 뷰
│   ├── OverlayView.kt         # 카메라 프리뷰 위 바운딩 박스 및 핑거 포인트 렌더링
│   └── SplashClockView.kt     # 스플래시 화면 애니메이션 시계 뷰
└── model/                     # 데이터 모델 및 정적 데이터베이스
    ├── DetectionResult.kt     # 탐지 객체 데이터 클래스
    └── MenuDatabase.kt        # 텍스트 매칭용 카페 메뉴 정적 DB

2. 컴포넌트 상세 역할
📱 Activities (화면 및 생명주기 관리)
SplashActivity.kt

앱 최초 진입 시 사용자 경험을 향상시키기 위한 로딩 화면입니다. SplashClockView를 통해 2.5초간 시계 바늘이 회전하는 애니메이션을 제공한 후, 핵심 기능을 담당하는 CameraActivity로 안전하게 전환합니다.

CameraActivity.kt

애플리케이션의 핵심 파이프라인을 관장하는 컨트롤러입니다. 내부적으로 SCANNING(탐지) → PROCESSING(연산) → INTERACTION(상호작용)의 3단계 유한 상태 기계(Finite State Machine)를 관리합니다. 카메라 하드웨어 바인딩, 자동 초점(AF) 잠금, 각 ML 모듈의 조율, 사용자의 손가락 체류 감지(1초 dwell time 판정) 및 햅틱 진동 피드백 출력을 총괄합니다.

🤖 ml/ (머신러닝 및 컴퓨터 비전)
KioskDetector.kt

온디바이스 추론을 위해 양자화된 TFLite YOLO 모델(kiosk_yolo_int8.tflite)을 실행합니다. 키오스크 화면 내의 버튼 및 UI 요소를 실시간으로 탐지하며, 중복 박스를 제거하기 위한 NMS(Non-Maximum Suppression) 및 내포 박스(상위 요소 내에 완전히 포함된 박스) 제거 알고리즘이 적용되어 있습니다.

FingerTracker.kt

실시간 스트리밍 모드(LIVE_STREAM)로 설정된 MediaPipe HandLandmarker를 구동합니다. 사용자의 검지 손가락 끝에 해당하는 8번 랜드마크를 추적하여 화면 좌표계 기준 0.0 ~ 1.0 사이의 정규화된 좌표를 콜백으로 반환합니다.

OpticalFlowTracker.kt

OpenCV의 Lucas-Kanade 알고리즘 기반 추적 기술을 활용하여 연속적인 프레임 간의 카메라 이동량을 계산합니다. 사용자가 키오스크 앞에서 손을 미세하게 떨더라도, 기존에 탐지된 바운딩 박스들이 흔들림에 따라 밀려나지 않고 제자리를 유지하도록 좌표를 실시간 보정합니다.

MenuMatcher.kt

OCR을 통해 인식된 텍스트의 입력 오류를 처리합니다. MenuDatabase에 수록된 정적 메뉴명과 편집 거리(Levenshtein Distance) 유사도를 계산하여 가장 일치하는 메뉴를 탐색합니다. 오인식을 방지하기 위해 유사도 0.55 미만의 결과는 null 처리합니다.

🔤 ocr/ (문자 인식)
OcrManager.kt

Google ML Kit의 Korean OCR 라이브러리를 사용합니다. KioskDetector가 추출한 버튼 영역의 비트맵을 크롭하여 타겟 영역 내의 문자를 정밀하게 추출하고, 그 결과를 비동기 콜백으로 반환합니다.

🔊 tts/ (음성 가이드)
TtsManager.kt

Android 시스템 내장 TextToSpeech 엔진의 성능을 최적화한 얇은 래퍼 클래스입니다. 시각 사각지대 사용자를 위한 오디오 피드백인 speak(), stop(), 리소스 해제를 위한 shutdown() 인터페이스를 투명하게 제공합니다.

🎨 view/ (커스텀 렌더링 뷰)
OverlayView.kt

카메라 화면 위에 오버레이되는 투명 캔버스 뷰입니다. 사용자의 직관적인 조작을 돕기 위해 뷰파인더 코너 브래킷, 동심원 및 글로우 라인 형태의 스캔 애니메이션, 버튼 영역의 바운딩 박스, 손가락 체류(Dwell) 시간을 시각화하는 원호(Arc), 실시간 검지 포인트를 그래픽으로 렌더링합니다.

SplashClockView.kt

스플래시 화면 전용 커스텀 뷰로, 2.5초 동안 정밀하게 제어되는 시계 바늘 회전 애니메이션 연산을 수행합니다.

📊 model/ (데이터 엔티티 및 저장소)
DetectionResult.kt

YOLO가 탐지한 객체의 기하학적 좌표 정보(RectF), 모델 신뢰도(Float), 그리고 매칭 완료된 OCR 텍스트(String)를 하나로 묶어 파이프라인 간 전달하기 위한 불변 데이터 구조(Data Class)입니다.

MenuDatabase.kt

배리어 프리 키오스크가 타겟으로 하는 카페 환경의 핵심 메뉴 정보(이름, 가격, 카테고리) 27종이 수록된 경량 정적 데이터베이스입니다. MenuMatcher의 유사도 연산 표준 데이터셋으로 활용됩니다.

3. 핵심 데이터 파이프라인
📸 A. 정적 스캔 및 텍스트 매칭 파이프라인 (초기 1회 및 화면 전환 시)
키오스크 화면이 캡처되면 시스템은 영역 내부의 텍스트 정보와 기하 정보의 구조화를 시작합니다.

KioskDetector가 이미지 내 버튼 오브젝트들을 일괄 탐지하여 바운딩 박스 리스트를 생성합니다.

각 바운딩 박스 영역은 OcrManager로 전달되어 해당 UI 내에 적힌 날것(Raw) 상태의 텍스트가 추출됩니다.

추출된 텍스트는 MenuMatcher를 거치며 오타가 보정되고 MenuDatabase 내의 정식 메뉴 엔티티와 결합합니다.

최종 정제된 데이터는 DetectionResult 객체들로 변환되어 OverlayView를 통해 화면에 사각형 박스 레이아웃으로 증강됩니다.

⏱️ B. 실시간 인터랙션 및 피드백 파이프라인 (INTERACTION 상태 유지 시)
사용자가 화면에 손을 대고 조작하는 순간부터 미세 제어 및 다중 모달 피드백 루프가 활성화됩니다.

카메라 움직임 대응: 기기가 흔들려도 OpticalFlowTracker가 프레임 간 변위 정보를 지속적으로 계산하여 기존 DetectionResult들의 바운딩 박스 좌표 추적 상태를 유지합니다.

손가락 추적 및 Dwell 감지: FingerTracker가 사용자의 검지 손가락 끝 좌표를 실시간 추적하고, CameraActivity는 이 포인트가 특정 바운딩 박스 내부에 1초 이상 머무르는지(Dwell Time) 연산합니다.

멀티모달 피드백 구현: 손가락이 버튼에 닿거나 Dwell 조건이 충족되면 즉각적으로 기기 진동 모터가 구동됨과 동시에, TtsManager를 통해 매칭된 메뉴 정보와 가격이 음성으로 출력되어 사각지대 없는 배리어 프리 UI를 완성합니다.
