"""
YOLOv8 麻将牌识别 - 一键训练与部署脚本

=== 开源数据集 ===
1. Roboflow Universe (免费, Apache 2.0)
   https://universe.roboflow.com/rf-100-vl/mahjong-vtacs-mexax-m4vyu-sjtd
   2000+ 张图, 34 类标准麻将牌

2. Kaggle (免费)
   https://www.kaggle.com/datasets/shinz114514/mahjong-hand-photos-taken-with-mobile-camera
   手机拍摄的真实麻将牌照片

3. firc-dataset (付费 ¥120, 7643 张, 42 类)
   https://mbd.pub/o/bread/mbd-YZWbm5dqZQ==
   mAP@0.5 = 96.7%

=== 预训练模型 ===
CSDN 开源项目 (含 .pt + .onnx, YOLOv11):
   https://download.csdn.net/download/m0_51061483/91467575
   包含 nano/small/medium/large 四种尺寸

=== 使用方法 ===
# 方式1: 使用 Roboflow 免费数据集训练
python train_yolov8_mahjong.py train-roboflow --api-key YOUR_KEY

# 方式2: 使用本地数据集训练
python train_yolov8_mahjong.py train --dataset /path/to/dataset

# 方式3: 使用 Kaggle 数据集训练
python train_yolov8_mahjong.py train-kaggle

# 导出 TFLite
python train_yolov8_mahjong.py export --weights runs/detect/train/weights/best.pt

# 验证模型
python train_yolov8_mahjong.py validate --weights best.pt --dataset /path/to/dataset
"""

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

import yaml


LABELS_34 = [
    "WAN_1", "WAN_2", "WAN_3", "WAN_4", "WAN_5", "WAN_6", "WAN_7", "WAN_8", "WAN_9",
    "TIAO_1", "TIAO_2", "TIAO_3", "TIAO_4", "TIAO_5", "TIAO_6", "TIAO_7", "TIAO_8", "TIAO_9",
    "TONG_1", "TONG_2", "TONG_3", "TONG_4", "TONG_5", "TONG_6", "TONG_7", "TONG_8", "TONG_9",
    "FENG_DONG", "FENG_NAN", "FENG_XI", "FENG_BEI",
    "JIAN_ZHONG", "JIAN_FA", "JIAN_BAI",
]

LABELS_42 = LABELS_34 + [
    "HUA_CHUN", "HUA_XIA", "HUA_QIU", "HUA_DONG",
    "JI_MEI", "JI_LAN", "JI_ZHU", "JI_JU",
]

FIRC_LABEL_MAP = {
    "1B": "WAN_1", "2B": "WAN_2", "3B": "WAN_3", "4B": "WAN_4", "5B": "WAN_5",
    "6B": "WAN_6", "7B": "WAN_7", "8B": "WAN_8", "9B": "WAN_9",
    "1C": "TONG_1", "2C": "TONG_2", "3C": "TONG_3", "4C": "TONG_4", "5C": "TONG_5",
    "6C": "TONG_6", "7C": "TONG_7", "8C": "TONG_8", "9C": "TONG_9",
    "1D": "TIAO_1", "2D": "TIAO_2", "3D": "TIAO_3", "4D": "TIAO_4", "5D": "TIAO_5",
    "6D": "TIAO_6", "7D": "TIAO_7", "8D": "TIAO_8", "9D": "TIAO_9",
    "EW": "FENG_DONG", "SW": "FENG_NAN", "WW": "FENG_XI", "NW": "FENG_BEI",
    "RD": "JIAN_ZHONG", "GD": "JIAN_FA", "WD": "JIAN_BAI",
    "1F": "HUA_CHUN", "2F": "HUA_XIA", "3F": "HUA_QIU", "4F": "HUA_DONG",
    "1S": "JI_MEI", "2S": "JI_LAN", "3S": "JI_ZHU", "4S": "JI_JU",
}


def check_dependencies():
    try:
        import ultralytics
        print(f"ultralytics {ultralytics.__version__} OK")
    except ImportError:
        print("Installing ultralytics...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "ultralytics"])


def create_dataset_yaml(dataset_dir: str, output_path: str = "mahjong_data.yaml", num_classes: int = 42):
    dataset_path = Path(dataset_dir).resolve()
    labels = LABELS_42 if num_classes == 42 else LABELS_34

    data = {
        "path": str(dataset_path),
        "train": "images/train",
        "val": "images/val",
        "test": "images/test",
        "names": {i: name for i, name in enumerate(labels)},
        "nc": num_classes,
    }

    with open(output_path, "w", encoding="utf-8") as f:
        yaml.dump(data, f, allow_unicode=True, default_flow_style=False)

    print(f"Dataset YAML created: {output_path}")
    return output_path


def download_roboflow(api_key: str, output_dir: str = "mahjong_dataset"):
    try:
        from roboflow import Roboflow
    except ImportError:
        print("Installing roboflow...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "roboflow"])
        from roboflow import Roboflow

    rf = Roboflow(api_key=api_key)
    project = rf.workspace("rf-100-vl").project("mahjong-vtacs-mexax-m4vyu-sjtd")
    version = project.version(2)
    dataset = version.download("yolov8", location=output_dir)
    print(f"Roboflow dataset downloaded to: {dataset.location}")
    return dataset.location


def download_kaggle(output_dir: str = "mahjong_dataset_kaggle"):
    kaggle_dir = Path(output_dir)
    kaggle_dir.mkdir(parents=True, exist_ok=True)

    print("Kaggle dataset download instructions:")
    print("1. Install kaggle CLI: pip install kaggle")
    print("2. Set up API key: https://www.kaggle.com/docs/api")
    print(f"3. Run: kaggle datasets download -d shinz114514/mahjong-hand-photos-taken-with-mobile-camera -p {output_dir}")
    print("4. Unzip and organize into YOLO format")
    print()
    print("Or download manually from:")
    print("https://www.kaggle.com/datasets/shinz114514/mahjong-hand-photos-taken-with-mobile-camera")

    try:
        subprocess.run([
            sys.executable, "-m", "kaggle", "datasets", "download",
            "-d", "shinz114514/mahjong-hand-photos-taken-with-mobile-camera",
            "-p", output_dir, "--unzip"
        ], check=False)
    except Exception as e:
        print(f"Auto-download failed: {e}")
        print("Please download manually using the URL above.")

    return output_dir


def convert_firc_labels(dataset_dir: str):
    labels_dir = Path(dataset_dir) / "labels"
    classes_file = labels_dir / "classes.txt"

    if not classes_file.exists():
        print("No classes.txt found, skipping FIRC label conversion")
        return

    with open(classes_file, "r") as f:
        original_labels = [line.strip() for line in f if line.strip()]

    label_map = {}
    for idx, orig_label in enumerate(original_labels):
        mapped = FIRC_LABEL_MAP.get(orig_label)
        if mapped:
            label_map[idx] = LABELS_42.index(mapped)
        else:
            print(f"Warning: Unknown label '{orig_label}' at index {idx}")

    for split in ["train", "val", "test"]:
        split_dir = labels_dir / split
        if not split_dir.exists():
            continue
        for txt_file in split_dir.glob("*.txt"):
            lines = txt_file.read_text().strip().split("\n")
            new_lines = []
            for line in lines:
                if not line.strip():
                    continue
                parts = line.strip().split()
                if len(parts) >= 5:
                    old_class = int(parts[0])
                    new_class = label_map.get(old_class, old_class)
                    parts[0] = str(new_class)
                    new_lines.append(" ".join(parts))
            txt_file.write_text("\n".join(new_lines) + "\n")

    new_classes = "\n".join(LABELS_42) + "\n"
    classes_file.write_text(new_classes)
    print(f"FIRC labels converted to standard format ({len(LABELS_42)} classes)")


def train(
    dataset_dir: str,
    epochs: int = 100,
    batch_size: int = 16,
    img_size: int = 640,
    model_size: str = "n",
    device: str = "",
    project: str = "runs/detect",
    name: str = "train",
    num_classes: int = 42,
):
    from ultralytics import YOLO

    yaml_path = create_dataset_yaml(dataset_dir, num_classes=num_classes)

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
        candidates = list(Path(weights).parent.rglob("*.tflite"))
        if candidates:
            tflite_path = candidates[0]

    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        dest = Path(output_dir) / "mahjong_yolov8.tflite"
        if tflite_path.exists():
            shutil.copy2(tflite_path, dest)
            print(f"TFLite model copied to: {dest}")
        else:
            print(f"Warning: TFLite model not found at {tflite_path}")

    android_assets = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    if android_assets.exists() and tflite_path.exists():
        dest = android_assets / "mahjong_yolov8.tflite"
        shutil.copy2(tflite_path, dest)
        print(f"TFLite model auto-copied to Android assets: {dest}")

    print(f"\nTFLite model exported to: {tflite_path}")
    print("Copy to: app/src/main/assets/mahjong_yolov8.tflite")
    return str(tflite_path)


def validate(weights: str, dataset_dir: str, num_classes: int = 42):
    from ultralytics import YOLO

    yaml_path = create_dataset_yaml(dataset_dir, num_classes=num_classes)
    model = YOLO(weights)

    metrics = model.val(data=yaml_path)

    print(f"\nValidation Results:")
    print(f"  mAP@0.5:      {metrics.box.map50:.4f}")
    print(f"  mAP@0.5:0.95: {metrics.box.map:.4f}")
    print(f"  Precision:    {metrics.box.mp:.4f}")
    print(f"  Recall:       {metrics.box.mr:.4f}")
    return metrics


def main():
    parser = argparse.ArgumentParser(description="YOLOv8 Mahjong Tile Detection - Train & Deploy")
    subparsers = parser.add_subparsers(dest="command", help="Command")

    train_parser = subparsers.add_parser("train", help="Train with local dataset")
    train_parser.add_argument("--dataset", type=str, required=True)
    train_parser.add_argument("--epochs", type=int, default=100)
    train_parser.add_argument("--batch", type=int, default=16)
    train_parser.add_argument("--img-size", type=int, default=640)
    train_parser.add_argument("--model-size", type=str, default="n", choices=["n", "s", "m", "l", "x"])
    train_parser.add_argument("--device", type=str, default="")
    train_parser.add_argument("--num-classes", type=int, default=42)
    train_parser.add_argument("--firc", action="store_true", help="Auto-convert FIRC dataset labels")

    rf_parser = subparsers.add_parser("train-roboflow", help="Train with Roboflow dataset")
    rf_parser.add_argument("--api-key", type=str, required=True)
    rf_parser.add_argument("--epochs", type=int, default=100)
    rf_parser.add_argument("--batch", type=int, default=16)
    rf_parser.add_argument("--model-size", type=str, default="n", choices=["n", "s", "m", "l", "x"])
    rf_parser.add_argument("--device", type=str, default="")

    kg_parser = subparsers.add_parser("train-kaggle", help="Train with Kaggle dataset")
    kg_parser.add_argument("--epochs", type=int, default=100)
    kg_parser.add_argument("--batch", type=int, default=16)
    kg_parser.add_argument("--model-size", type=str, default="n", choices=["n", "s", "m", "l", "x"])
    kg_parser.add_argument("--device", type=str, default="")

    export_parser = subparsers.add_parser("export", help="Export to TFLite")
    export_parser.add_argument("--weights", type=str, required=True)
    export_parser.add_argument("--img-size", type=int, default=640)
    export_parser.add_argument("--output-dir", type=str, default="")
    export_parser.add_argument("--int8", action="store_true")
    export_parser.add_argument("--data-yaml", type=str, default="")

    val_parser = subparsers.add_parser("validate", help="Validate model")
    val_parser.add_argument("--weights", type=str, required=True)
    val_parser.add_argument("--dataset", type=str, required=True)
    val_parser.add_argument("--num-classes", type=int, default=42)

    args = parser.parse_args()
    check_dependencies()

    if args.command == "train":
        if args.firc:
            convert_firc_labels(args.dataset)
        train(
            dataset_dir=args.dataset,
            epochs=args.epochs,
            batch_size=args.batch,
            img_size=args.img_size,
            model_size=args.model_size,
            device=args.device,
            num_classes=args.num_classes,
        )
    elif args.command == "train-roboflow":
        dataset_dir = download_roboflow(args.api_key)
        train(
            dataset_dir=dataset_dir,
            epochs=args.epochs,
            batch_size=args.batch,
            model_size=args.model_size,
            device=args.device,
            num_classes=34,
        )
    elif args.command == "train-kaggle":
        dataset_dir = download_kaggle()
        train(
            dataset_dir=dataset_dir,
            epochs=args.epochs,
            batch_size=args.batch,
            model_size=args.model_size,
            device=args.device,
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
            num_classes=args.num_classes,
        )
    else:
        parser.print_help()
        print("\n" + __doc__)


if __name__ == "__main__":
    main()
