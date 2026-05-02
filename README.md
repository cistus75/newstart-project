<<<<<<< HEAD
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
=======
# Team new-start : Project by AI and Vision

경기대학교 상상기업 & 캡스톤 디자인 기반 팀 프로젝트

우리는 기술을 통해 사각지대에 놓인 이들을 위해 시각의 벽을 허물어 새로운 시작을 지향합니다.

-- 

## Project Context

본 프로젝트는 **경기대학교 상상기업 및 캡스톤 디자인** 과정을 기반으로 진행됩니다. 
단순한 코드 구현을 넘어, 실제 시장의 접근성 문제를 해결하는 MVP 수준의 개발을 목표로 합니다.

* 소속 : 경기대학교
* 과정 : 상상기업 / 캡스톤 디자인
* 팀명 : NewStart

## Project Overview 

우리 팀은 배리어 프리 키오스크 솔루션을 목표로 합니다.
기존 키오스크의 물리적인 교체 없이 AI 비전 기술을 활용해 저시력자의 서비스 이용을 돕습니다.

1. AI Vision Recognition : YOLOv8을 기반으로 하여 키오스크 화면의 요소를 실시간으로 탐색합니다.
2. Touch Guide : MediaPipe를 활용해 사용자의 손가락을 추적하여 어떤 버튼에 있는지 탐색합니다.
3. Multimodal Feedback : TTS & Haptic을 통해 보이지 않아도 직관적으로 조작이 가능한 UX를 구현합니다.


## Commit Convention

커밋 메시지는 작업 성격을 한 눈에 알 수 있도록 아래의 Prefix를 반드시 적용합니다. 
해당 규칙을 준수하지 않는 커밋은 리젝될 수 있습니다.

* 'feat:' : 새로운 기능 추가 시 사용합니다. (Ex : feat: 하단 버튼 터치 기능 #1)
* 이슈 번호(#)는 GitHub Issues 탭에서 생성된 번호를 사용하며 커밋 메시지 끝에 한 칸 띄우고 붙입니다.
* 'fix:' : 버그 수정 시 사용합니다. (Ex : fix: 하단 버튼 터치 불가능 버그 수정 #2)
* 'typo:' : 오타 및 사소한 변경사항 시 사용합니다. (Ex : typo: prnit > print 오타 수정 및 세미콜론 누락 수정 #3)
* `refactor:' : 코드 리팩토링 시 사용합니다. (Ex : refactor: 연산 효율을 위해 코드 구조 변경 #4)
* 'docs:' : README를 비롯한 문서 수정 시 사용합니다. (Ex :  docs: update readme for Commit Convention #5)
* 절대 main 브랜치에 커밋하지 않습니다. 반드시 PR을 통한 merge만을 사용합니다.

## Issue & PR Convention

모든 작업을 이슈 발행 이후 진행합니다. 
해당 규칙을 준수하지 않는 커밋은 리젝될 수 있습니다.

* '[태그] 작업 내용' 형식으로 작성합니다.
* Prefix는 Commit Convention과 동일하게 사용합니다.
* Content에 CheckList를 포함합니다.

### PR 작업 시

* '[태그] 작업 내용 (#이슈 번호)' 형식으로 작성합니다.
* Content에 작업 내용을 간단히 서술합니다
* 'Closes #이슈 번호'를 사용하여 해결된 이슈를 자동으로 닫습니다.

---

* Tech Stack : https://www.notion.so/5b111acc0b5f4cabb94ee2adb12e0696
* Communication : Discord & KakaoTalk

---
© 2026 Team New-Start. All rights reserved.
>>>>>>> a93033ba7d93f726e5b7e6050f33d55a4b85e227
