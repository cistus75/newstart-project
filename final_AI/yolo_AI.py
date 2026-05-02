"""
키오스크 UI 컴포넌트 탐지 모듈 (Kiosk UI Component Detector)

학습된 YOLOv8n 모델을 사용하여 키오스크 화면 내 UI 요소를 탐지하고,
바운딩 박스 좌표를 JSON 형태로 반환합니다.

탐지 클래스:
  - action_button  : 화면 전환/실행 버튼 (주문담기, 결제하기, 취소, 전체삭제)
  - cart_item      : 장바구니 항목 (메뉴명 + 수량 + 가격 한 줄)
  - category_tab   : 상단 카테고리 탭
  - menu_back      : 메뉴 아이템 전체 영역 (이미지 + 텍스트 + 배지 포함)
  - option_button  : 팝업 내 속성 선택 버튼 (ICE/HOT, 샷추가 등)
  - popup_modal    : 화면 중앙 팝업창 전체 영역

제외 클래스 (Google ML Kit OCR로 처리):
  - menu_name      : 메뉴 상품명 텍스트
  - menu_price     : 메뉴 가격 텍스트
"""

import os
import json
import argparse
import cv2
from ultralytics import YOLO

# 탐지에서 제외할 클래스 (텍스트 영역 → ML Kit에서 처리)
EXCLUDED_CLASSES = {4, 5}  # 4: menu_name, 5: menu_price

# 클래스 ID → 이름 매핑
CLASS_NAMES = {
    0: 'action_button',
    1: 'cart_item',
    2: 'category_tab',
    3: 'menu_back',
    4: 'menu_name',
    5: 'menu_price',
    6: 'option_button',
    7: 'popup_modal'
}

# 클래스별 바운딩 박스 색상 (BGR)
CLASS_COLORS = {
    0: (255, 0, 0),    # action_button  - 파랑
    1: (0, 165, 255),  # cart_item      - 주황
    2: (0, 0, 255),    # category_tab   - 빨강
    3: (0, 255, 0),    # menu_back      - 초록
    6: (0, 0, 255),    # option_button  - 빨강
    7: (255, 0, 255),  # popup_modal    - 보라
}

DEFAULT_MODEL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "weights", "best.pt")


def detect_kiosk_ui(image_path, model_path=None, conf=0.7, iou=0.45, save_image=False):
    """
    키오스크 이미지에서 UI 컴포넌트를 탐지합니다.

    Args:
        image_path (str): 입력 이미지 경로
        model_path (str): YOLO 모델 파일 경로 (.pt 또는 .tflite)
        conf (float): 최소 신뢰도 임계값 (0.0 ~ 1.0)
        iou (float): NMS IOU 임계값 (겹침 제거 기준)
        save_image (bool): 바운딩 박스가 그려진 이미지를 저장할지 여부

    Returns:
        list[dict]: 탐지된 UI 컴포넌트 리스트
            각 항목: {"class", "confidence", "coordinates": {"x_min", "y_min", "x_max", "y_max"}}
    """
    if model_path is None:
        model_path = DEFAULT_MODEL

    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model not found at {model_path}")

    model = YOLO(model_path)
    results = model(image_path, verbose=False, conf=conf, iou=iou)

    output = []

    # 시각화용 이미지 로드 (save_image가 True일 때만)
    img = cv2.imread(image_path) if save_image else None

    for result in results:
        for box in result.boxes:
            cls_id = int(box.cls[0].item())
            if cls_id in EXCLUDED_CLASSES:
                continue

            coords = box.xyxy[0].tolist()
            confidence = float(box.conf[0].item())
            class_name = CLASS_NAMES.get(cls_id, f"class_{cls_id}")

            output.append({
                "class": class_name,
                "confidence": round(confidence, 4),
                "coordinates": {
                    "x_min": round(coords[0], 2),
                    "y_min": round(coords[1], 2),
                    "x_max": round(coords[2], 2),
                    "y_max": round(coords[3], 2)
                }
            })

            # 바운딩 박스 시각화
            if img is not None:
                x1, y1, x2, y2 = map(int, coords)
                color = CLASS_COLORS.get(cls_id, (128, 128, 128))
                cv2.rectangle(img, (x1, y1), (x2, y2), color, 3)
                label = f"{class_name} {confidence:.2f}"
                cv2.putText(img, label, (x1, max(y1 - 10, 0)),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

    if img is not None:
        out_path = os.path.splitext(image_path)[0] + "_result.jpg"
        cv2.imwrite(out_path, img)
        print(f"Result image saved to {out_path}")

    return output


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="키오스크 UI 컴포넌트 탐지기")
    parser.add_argument('image_path', help="입력 이미지 경로")
    parser.add_argument('--model', default=None, help="YOLO 모델 경로 (.pt 또는 .tflite)")
    parser.add_argument('--conf', type=float, default=0.7, help="최소 신뢰도 (기본값: 0.7)")
    parser.add_argument('--iou', type=float, default=0.45, help="NMS IOU 임계값 (기본값: 0.45)")
    parser.add_argument('--save-image', action='store_true', help="결과 이미지 저장")
    args = parser.parse_args()

    try:
        detections = detect_kiosk_ui(
            args.image_path,
            model_path=args.model,
            conf=args.conf,
            iou=args.iou,
            save_image=args.save_image
        )
        print(json.dumps(detections, indent=2, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({"error": str(e)}, ensure_ascii=False))
