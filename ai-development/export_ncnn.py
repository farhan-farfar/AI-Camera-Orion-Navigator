#!/usr/bin/env python3
"""
Orion Navigator - AI Model Export to NCNN Format
Exports YOLO model (e.g. YOLO26n) to NCNN format for on-device Android inference.
"""

import os
import sys
import shutil
import argparse
from pathlib import Path

try:
    from ultralytics import YOLO
except ImportError:
    print("[ERROR] Ultralytics package not found.")
    print("Please run: pip install -r requirements.txt")
    sys.exit(1)


def export_model(model_name: str = "yolo26n.pt", imgsz: int = 320, copy_to_assets: bool = True):
    print("=" * 60)
    print(" Orion Navigator - YOLO to NCNN Model Exporter")
    print("=" * 60)
    print(f" Target Model : {model_name}")
    print(f" Input Size   : {imgsz} x {imgsz}")
    print("=" * 60)

    # 1. Load Model
    print(f"\n[1/4] Loading model: {model_name}...")
    try:
        model = YOLO(model_name)
    except Exception as e:
        print(f"[ERROR] Failed to load model '{model_name}': {e}")
        print("Note: If yolo26n is not yet in public weights, consider training or using yolo11n.pt as baseline.")
        sys.exit(1)

    # 2. Export to NCNN
    print(f"\n[2/4] Exporting to NCNN (imgsz={imgsz})...")
    try:
        # NCNN export from Ultralytics
        export_path = model.export(format="ncnn", imgsz=imgsz)
        print(f"[SUCCESS] Export completed: {export_path}")
    except Exception as e:
        print(f"[ERROR] Export failed: {e}")
        sys.exit(1)

    # 3. Verify output files
    print("\n[3/4] Verifying exported files...")
    export_dir = Path(export_path)
    if not export_dir.is_dir():
        export_dir = export_dir.parent

    param_file = None
    bin_file = None

    for f in export_dir.glob("*.param"):
        param_file = f
        break
    for f in export_dir.glob("*.bin"):
        bin_file = f
        break

    if not param_file or not bin_file:
        print(f"[ERROR] Expected .param and .bin files in {export_dir}")
        print(f"Found: {list(export_dir.glob('*'))}")
        sys.exit(1)

    print(f" - Found param file: {param_file.name} ({param_file.stat().st_size} bytes)")
    print(f" - Found bin file  : {bin_file.name} ({bin_file.stat().st_size} bytes)")

    # 4. Copy to Android Assets
    if copy_to_assets:
        target_dir = Path(__file__).resolve().parent.parent / "OrionNavigator" / "app" / "src" / "main" / "assets" / "models"
        print(f"\n[4/4] Copying to Android assets: {target_dir}...")
        target_dir.mkdir(parents=True, exist_ok=True)

        dest_param = target_dir / "model.ncnn.param"
        dest_bin = target_dir / "model.ncnn.bin"

        shutil.copy2(param_file, dest_param)
        shutil.copy2(bin_file, dest_bin)

        print(f" [OK] Copied -> {dest_param}")
        print(f" [OK] Copied -> {dest_bin}")
    else:
        print("\n[4/4] Skipping copy to Android assets (--no-copy specified).")

    print("\n" + "=" * 60)
    print(" EXPORT PIPELINE FINISHED SUCCESSFULLY!")
    print("=" * 60)
    print("Your Android app is now ready to use this NCNN model offline.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export YOLO model to NCNN for Orion Navigator")
    parser.add_argument("--model", type=str, default="yolo26n.pt", help="Path or name of YOLO model (default: yolo26n.pt)")
    parser.add_argument("--imgsz", type=int, default=320, help="Inference image resolution (default: 320)")
    parser.add_argument("--no-copy", action="store_true", help="Do not copy exported files to Android assets folder")

    args = parser.parse_args()
    export_model(model_name=args.model, imgsz=args.imgsz, copy_to_assets=not args.no_copy)
