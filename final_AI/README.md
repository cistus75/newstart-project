# Kiosk Guide AI

**온디바이스 키오스크 안내 시스템**의 핵심 AI 모듈입니다.  
YOLOv8n 기반 객체 탐지를 통해 키오스크 화면 내 다양한 UI 요소를 실시간으로 파악하여, 탐지된 좌표 데이터를 제공하여 키오스크 이용을 돕습니다.

---

## 주요 기능

- **UI 컴포넌트 실시간 탐지**: 메뉴, 버튼, 카테고리 등 키오스크 필수 요소 탐지
- **좌표 데이터 출력 (JSON)**: 모바일 앱 연동을 위한 정밀 좌표값 제공
- **온디바이스 최적화**: TFLite INT8 양자화 지원으로 모바일 환경에서도 빠른 속도 보장

---

## 탐지 클래스 및 역할

| 클래스 | 역할 설명 |
|--------|------|
| `menu_back` | 메뉴 아이템 전체 영역 |
| `category_tab` | 상단 카테고리 메뉴 |
| `action_button` | 주문담기, 결제하기, 취소, 전체삭제 등 핵심 실행 버튼 |
| `option_button` | ICE/HOT, 샷추가 등 메뉴 옵션 선택 버튼 |
| `popup_modal` | 팝업창 전체 영역 |
| `cart_item` | 장바구니에 담긴 항목 확인 |

> 💡 **참고**: 메뉴명과 가격 텍스트는 별도의 OCR 모듈에서 처리할 수 있도록 좌표를 함께 제공합니다.

---

## 기술 스펙

- **Model**: YOLOv8n (Nano)
- **Dataset**: 통합 387장 (디지털 캡처 + 실사 촬영 정제 데이터)
- **Performance**: mAP50 **97.8%** 달성
- **HW Acceleration**: NVIDIA RTX 4070 Ti 활용 (CUDA 11.8)
- **Dependencies**: ultralytics, opencv-python, torch, pyyaml
