#!/usr/bin/env python3
"""
Orion Navigator - Test AI Object Detection Model
Evaluates YOLO model predictions and validates Indonesian text output format.
"""

import sys
import argparse
from pathlib import Path

try:
    from ultralytics import YOLO
except ImportError:
    print("[ERROR] Ultralytics package not found. Run: pip install -r requirements.txt")
    sys.exit(1)


INDONESIAN_TRANSLATIONS = {
    "person": "manusia",
    "bicycle": "sepeda",
    "car": "mobil",
    "motorcycle": "sepeda motor",
    "bus": "bus",
    "truck": "truk",
    "traffic light": "lampu lalu lintas",
    "fire hydrant": "hidran",
    "stop sign": "rambu berhenti",
    "bench": "bangku",
    "cat": "kucing",
    "dog": "anjing",
    "backpack": "tas ransel",
    "umbrella": "payung",
    "handbag": "tas tangan",
    "suitcase": "koper",
    "bottle": "botol",
    "cup": "gelas",
    "fork": "garpu",
    "knife": "pisau",
    "spoon": "sendok",
    "bowl": "mangkuk",
    "chair": "kursi",
    "couch": "sofa",
    "potted plant": "tanaman pot",
    "bed": "tempat tidur",
    "dining table": "meja makan",
    "toilet": "toilet",
    "tv": "televisi",
    "laptop": "laptop",
    "cell phone": "ponsel",
    "book": "buku",
    "clock": "jam",
    "scissors": "gunting",
    "toothbrush": "sikat gigi",
    "door": "pintu"
}

INDONESIAN_NUMBERS = [
    "", "satu", "dua", "tiga", "empat", "lima",
    "enam", "tujuh", "delapan", "sembilan", "sepuluh"
]


def format_tts_sentence(labels: list[str]) -> str:
    if not labels:
        return "Maaf, saya belum dapat mengenali objek pada gambar. Silakan coba mengambil gambar kembali."

    if len(labels) == 1:
        name = INDONESIAN_TRANSLATIONS.get(labels[0], labels[0])
        return f"Saya mendeteksi sebuah {name} di depan kamera."

    # Group counts
    counts = {}
    for label in labels:
        counts[label] = counts.get(label, 0) + 1

    parts = []
    for label, count in counts.items():
        name = INDONESIAN_TRANSLATIONS.get(label, label)
        num_str = INDONESIAN_NUMBERS[count] if 1 <= count <= 10 else str(count)
        parts.append(f"{num_str} {name}")

    if len(parts) == 1:
        listing = parts[0]
    elif len(parts) == 2:
        listing = f"{parts[0]} dan {parts[1]}"
    else:
        listing = f"{', '.join(parts[:-1])}, dan {parts[-1]}"

    return f"Saya mendeteksi {len(labels)} objek. Terdapat {listing}."


def test_inference(model_path: str, image_path: str, conf_threshold: float = 0.45):
    print("=" * 60)
    print(" Orion Navigator - Model Inference Test")
    print("=" * 60)
    print(f" Model       : {model_path}")
    print(f" Test Image  : {image_path}")
    print(f" Confidence  : {conf_threshold}")
    print("=" * 60)

    model = YOLO(model_path)
    results = model.predict(source=image_path, conf=conf_threshold, imgsz=320)

    detected_labels = []

    for r in results:
        boxes = r.boxes
        print(f"\n[Detections Found: {len(boxes)}]")
        for i, box in enumerate(boxes):
            cls_id = int(box.cls[0])
            label_en = model.names[cls_id]
            label_id = INDONESIAN_TRANSLATIONS.get(label_en, label_en)
            conf = float(box.conf[0])
            coords = [round(float(c), 1) for c in box.xyxy[0]]

            detected_labels.append(label_en)
            print(f" #{i+1:02d}: {label_en:<15} ({label_id}) | Conf: {conf*100:.1f}% | Box: {coords}")

    print("\n" + "-" * 60)
    print(" SIMULATED TEXT-TO-SPEECH (TTS) OUTPUT (BAHASA INDONESIA):")
    print("-" * 60)
    sentence = format_tts_sentence(detected_labels)
    print(f"\"{sentence}\"")
    print("-" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Test YOLO model inference for Orion Navigator")
    parser.add_argument("--model", type=str, default="yolo26n.pt", help="Model path (default: yolo26n.pt)")
    parser.add_argument("--image", type=str, required=True, help="Path to test image")
    parser.add_argument("--conf", type=float, default=0.45, help="Confidence threshold (default: 0.45)")

    args = parser.parse_args()
    test_inference(args.model, args.image, args.conf)
