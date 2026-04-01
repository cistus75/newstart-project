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
