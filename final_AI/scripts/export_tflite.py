# TFLite 양자화 변환 스크립트
# 학습 완료된 best.pt 모델을 온디바이스 추론용 TFLite INT8 포맷으로 변환

import os
from ultralytics import YOLO


def export_model():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    best_weights = os.path.join(base_dir, '..', 'weights', 'best.pt')

    if not os.path.exists(best_weights):
        print(f"Error: {best_weights} not found. Train the model first.")
        return

    model = YOLO(best_weights)

    print(f"Exporting {best_weights} → TFLite INT8...")

    # 대표 데이터셋 경로 (양자화 보정용)
    data_yaml = os.path.join(base_dir, '..', '..', 'combined_dataset', 'data.yaml')

    model.export(
        format='tflite',
        int8=True,
        data=data_yaml,
        imgsz=640
    )

    print("Export complete!")


if __name__ == '__main__':
    export_model()
