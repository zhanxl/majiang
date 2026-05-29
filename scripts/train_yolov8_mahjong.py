"""
YOLOv8 麻将牌识别模型训练与 TFLite 导出脚本

使用方法:
1. 安装依赖: pip install ultralytics opencv-python pillow
2. 准备数据集 (见下方说明)
3. 训练模型: python train_yolov8_mahjong.py train
4. 导出TFLite: python train_yolov8_mahjong.py export --weights runs/detect/train/weights/best.pt
5. 将导出的 mahjong_yolov8.tflite 复制到 Android 项目的 assets 目录

数据集结构:
    mahjong_dataset/
    ├── images/
    │   ├── train/
    │   │   ├── img_001.jpg
    │   │   ├── img_002.jpg
    │   │   └── ...
    │   ├── val/
    │   │   ├── img_101.jpg
    │   │   └── ...
    │   └── test/
    │       └── ...
    └── labels/
        ├── train/
        │   ├── img_001.txt   (YOLO格式标注)
        │   ├── img_002.txt
        │   └── ...
        ├── val/
        │   ├── img_101.txt
        │   └── ...
        └── test/
            └── ...

标注格式 (YOLO, 每行一个目标):
    class_id center_x center_y width height
    例如: 0 0.5 0.3 0.1 0.15

类别ID对应 mahjong_labels.txt 中的顺序:
    0=WAN_1, 1=WAN_2, ..., 8=WAN_9
    9=TIAO_1, ..., 17=TIAO_9
    18=TONG_1, ..., 26=TONG_9
    27=FENG_DONG, 28=FENG_NAN, 29=FENG_XI, 30=FENG_BEI
    31=JIAN_ZHONG, 32=JIAN_FA, 33=JIAN_BAI

数据采集建议:
    - 每类至少 150-200 张标注图片
    - 总计 5000-10000 张图片
    - 包含不同光照、角度、背景
    - 包含不同品牌麻将牌面
    - 使用 LabelImg 或 Roboflow 标注
"""

import argparse
import os
import shutil
from pathlib import Path

import yaml


LABELS = [
    "WAN_1", "WAN_2", "WAN_3", "WAN_4", "WAN_5", "WAN_6", "WAN_7", "WAN_8", "WAN_9",
    "TIAO_1", "TIAO_2", "TIAO_3", "TIAO_4", "TIAO_5", "TIAO_6", "TIAO_7", "TIAO_8", "TIAO_9",
    "TONG_1", "TONG_2", "TONG_3", "TONG_4", "TONG_5", "TONG_6", "TONG_7", "TONG_8", "TONG_9",
    "FENG_DONG", "FENG_NAN", "FENG_XI", "FENG_BEI",
    "JIAN_ZHONG", "JIAN_FA", "JIAN_BAI",
]

NUM_CLASSES = len(LABELS)


def create_dataset_yaml(dataset_dir: str, output_path: str = "mahjong_data.yaml"):
    dataset_path = Path(dataset_dir).resolve()

    data = {
        "path": str(dataset_path),
        "train": "images/train",
        "val": "images/val",
        "test": "images/test",
        "names": {i: name for i, name in enumerate(LABELS)},
        "nc": NUM_CLASSES,
    }

    with open(output_path, "w", encoding="utf-8") as f:
        yaml.dump(data, f, allow_unicode=True, default_flow_style=False)

    print(f"Dataset YAML created: {output_path}")
    return output_path


def train(
    dataset_dir: str,
    epochs: int = 100,
    batch_size: int = 16,
    img_size: int = 640,
    model_size: str = "s",
    device: str = "",
    project: str = "runs/detect",
    name: str = "train",
):
    from ultralytics import YOLO

    yaml_path = create_dataset_yaml(dataset_dir)

    model_name = f"yolov8{model_size}.pt"
    model = YOLO(model_name)

    results = model.train(
        data=yaml_path,
        epochs=epochs,
        batch=batch_size,
        imgsz=img_size,
        device=device if device else None,
        project=project,
        name=name,
        patience=20,
        save=True,
        save_period=10,
        workers=8,
        augment=True,
        mosaic=1.0,
        mixup=0.1,
        copy_paste=0.1,
        degrees=15.0,
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        flipud=0.1,
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
    )

    print(f"\nTraining complete! Best weights: {project}/{name}/weights/best.pt")
    return results


def export_tflite(
    weights: str,
    img_size: int = 640,
    output_dir: str = "",
    int8: bool = False,
    data_yaml: str = "",
):
    from ultralytics import YOLO

    model = YOLO(weights)

    export_kwargs = {
        "format": "tflite",
        "imgsz": img_size,
    }

    if int8:
        if not data_yaml:
            print("Warning: --data-yaml required for int8 quantization, falling back to float16")
        else:
            export_kwargs["int8"] = True
            export_kwargs["data"] = data_yaml

    export_path = model.export(**export_kwargs)

    tflite_path = Path(export_path)
    if not tflite_path.exists():
        tflite_path = Path(weights).parent / f"{Path(weights).stem}_saved_model" / f"{Path(weights).stem}_float16.tflite"
        if not tflite_path.exists():
            tflite_path = Path(weights).parent / f"{Path(weights).stem}.tflite"

    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        dest = Path(output_dir) / "mahjong_yolov8.tflite"
        if tflite_path.exists():
            shutil.copy2(tflite_path, dest)
            print(f"TFLite model copied to: {dest}")
        else:
            print(f"Warning: TFLite model not found at {tflite_path}")
    else:
        print(f"TFLite model exported to: {tflite_path}")

    labels_src = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "mahjong_labels.txt"
    if not labels_src.exists():
        print(f"\nDon't forget to copy the TFLite model to:")
        print(f"  app/src/main/assets/mahjong_yolov8.tflite")

    return str(tflite_path)


def validate(weights: str, dataset_dir: str):
    from ultralytics import YOLO

    yaml_path = create_dataset_yaml(dataset_dir)
    model = YOLO(weights)

    metrics = model.val(data=yaml_path)

    print(f"\nValidation Results:")
    print(f"  mAP@0.5:    {metrics.box.map50:.4f}")
    print(f"  mAP@0.5:0.95: {metrics.box.map:.4f}")
    print(f"  Precision:  {metrics.box.mp:.4f}")
    print(f"  Recall:     {metrics.box.mr:.4f}")

    return metrics


def main():
    parser = argparse.ArgumentParser(description="YOLOv8 Mahjong Tile Detection Training")
    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    train_parser = subparsers.add_parser("train", help="Train YOLOv8 model")
    train_parser.add_argument("--dataset", type=str, required=True, help="Path to dataset directory")
    train_parser.add_argument("--epochs", type=int, default=100, help="Number of training epochs")
    train_parser.add_argument("--batch", type=int, default=16, help="Batch size")
    train_parser.add_argument("--img-size", type=int, default=640, help="Input image size")
    train_parser.add_argument("--model-size", type=str, default="s", choices=["n", "s", "m", "l", "x"], help="YOLOv8 model size")
    train_parser.add_argument("--device", type=str, default="", help="CUDA device (e.g., 0, 0,1, cpu)")
    train_parser.add_argument("--project", type=str, default="runs/detect", help="Project directory")
    train_parser.add_argument("--name", type=str, default="train", help="Experiment name")

    export_parser = subparsers.add_parser("export", help="Export model to TFLite")
    export_parser.add_argument("--weights", type=str, required=True, help="Path to trained weights (.pt)")
    export_parser.add_argument("--img-size", type=int, default=640, help="Input image size")
    export_parser.add_argument("--output-dir", type=str, default="", help="Output directory for TFLite model")
    export_parser.add_argument("--int8", action="store_true", help="Enable int8 quantization")
    export_parser.add_argument("--data-yaml", type=str, default="", help="Dataset YAML for int8 calibration")

    val_parser = subparsers.add_parser("validate", help="Validate trained model")
    val_parser.add_argument("--weights", type=str, required=True, help="Path to trained weights (.pt)")
    val_parser.add_argument("--dataset", type=str, required=True, help="Path to dataset directory")

    args = parser.parse_args()

    if args.command == "train":
        train(
            dataset_dir=args.dataset,
            epochs=args.epochs,
            batch_size=args.batch,
            img_size=args.img_size,
            model_size=args.model_size,
            device=args.device,
            project=args.project,
            name=args.name,
        )
    elif args.command == "export":
        export_tflite(
            weights=args.weights,
            img_size=args.img_size,
            output_dir=args.output_dir,
            int8=args.int8,
            data_yaml=args.data_yaml,
        )
    elif args.command == "validate":
        validate(
            weights=args.weights,
            dataset_dir=args.dataset,
        )
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
