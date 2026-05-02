# 키오스크 UI 탐지 모델 학습 스크립트
# 정제된 통합 데이터셋(combined_dataset)을 사용하여 YOLOv8n 모델을 학습시킵니다.

import os
from ultralytics import YOLO


def train():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    combined_yaml = os.path.join(base_dir, '..', '..', 'combined_dataset', 'data.yaml')

    print("학습 시작")
    model = YOLO("yolov8n.pt")

    model.train(
        data=combined_yaml,
        epochs=100,
        imgsz=640,
        batch=16,
        project="kiosk_yolo",
        name="combined_training"
    )

    print("학습 완료")


if __name__ == '__main__':
    train()
