#!/usr/bin/env python3
"""
Orion Navigator - Native Dependencies Downloader
Automates downloading and extracting prebuilt NCNN and OpenCV Mobile packages
for the Android NDK build (app/src/main/cpp).
"""

import os
import sys
import zipfile
import shutil
import urllib.request
from pathlib import Path

# URLs for latest prebuilt binaries (Tencent NCNN & nihui/opencv-mobile)
NCNN_URL = "https://github.com/Tencent/ncnn/releases/download/20241226/ncnn-20241226-android-vulkan.zip"
OPENCV_MOBILE_URL = "https://github.com/nihui/opencv-mobile/releases/download/v34/opencv-mobile-4.10.0-android.zip"

CPP_DIR = Path(__file__).resolve().parent.parent / "OrionNavigator" / "app" / "src" / "main" / "cpp"
DOWNLOAD_CACHE = Path(__file__).resolve().parent / "downloads"


def download_file(url: str, dest_path: Path):
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    if dest_path.exists() and dest_path.stat().st_size > 1024 * 1024:
        print(f"[CACHE] Using existing cached download: {dest_path.name}")
        return

    print(f"[DOWNLOAD] Downloading from: {url}")
    print(f"           Destination: {dest_path}")

    def progress_hook(block_num, block_size, total_size):
        downloaded = block_num * block_size
        if total_size > 0:
            percent = downloaded / total_size * 100
            mb_down = downloaded / (1024 * 1024)
            mb_tot = total_size / (1024 * 1024)
            sys.stdout.write(f"\r  -> Progress: {percent:.1f}% ({mb_down:.1f} MB / {mb_tot:.1f} MB)")
        else:
            sys.stdout.write(f"\r  -> Downloaded: {downloaded / (1024 * 1024):.1f} MB")
        sys.stdout.flush()

    opener = urllib.request.build_opener()
    opener.addheaders = [('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)')]
    urllib.request.install_opener(opener)
    urllib.request.urlretrieve(url, str(dest_path), reporthook=progress_hook)
    print("\n  -> Download finished!")


def extract_zip(zip_path: Path, target_dir: Path):
    print(f"[EXTRACT] Extracting {zip_path.name} to {target_dir}...")
    target_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(target_dir)
    print(f"[OK] Extracted successfully.")


def setup_ncnn():
    print("\n" + "=" * 60)
    print(" 1. Setting up NCNN Android Vulkan")
    print("=" * 60)
    zip_path = DOWNLOAD_CACHE / "ncnn-android-vulkan.zip"
    download_file(NCNN_URL, zip_path)

    target_ncnn_dir = CPP_DIR / "ncnn-android-vulkan"
    temp_extract = DOWNLOAD_CACHE / "ncnn_temp"

    if temp_extract.exists():
        shutil.rmtree(temp_extract)

    extract_zip(zip_path, temp_extract)

    # Move content: find the folder containing arm64-v8a inside zip
    subfolders = [f for f in temp_extract.iterdir() if f.is_dir()]
    source_folder = subfolders[0] if subfolders else temp_extract

    if target_ncnn_dir.exists():
        shutil.rmtree(target_ncnn_dir)

    shutil.move(str(source_folder), str(target_ncnn_dir))
    print(f"[DONE] NCNN installed to: {target_ncnn_dir}")


def setup_opencv():
    print("\n" + "=" * 60)
    print(" 2. Setting up OpenCV Mobile Android")
    print("=" * 60)
    zip_path = DOWNLOAD_CACHE / "opencv-mobile-android.zip"
    download_file(OPENCV_MOBILE_URL, zip_path)

    target_opencv_dir = CPP_DIR / "opencv-mobile"
    temp_extract = DOWNLOAD_CACHE / "opencv_temp"

    if temp_extract.exists():
        shutil.rmtree(temp_extract)

    extract_zip(zip_path, temp_extract)

    subfolders = [f for f in temp_extract.iterdir() if f.is_dir()]
    source_folder = subfolders[0] if subfolders else temp_extract

    if target_opencv_dir.exists():
        shutil.rmtree(target_opencv_dir)

    shutil.move(str(source_folder), str(target_opencv_dir))
    print(f"[DONE] OpenCV Mobile installed to: {target_opencv_dir}")


def verify_setup():
    print("\n" + "=" * 60)
    print(" 3. Verifying Installation")
    print("=" * 60)

    ncnn_cmake = CPP_DIR / "ncnn-android-vulkan" / "arm64-v8a" / "lib" / "cmake" / "ncnn" / "ncnnConfig.cmake"
    opencv_cmake = CPP_DIR / "opencv-mobile" / "sdk" / "native" / "jni" / "OpenCVConfig.cmake"

    ncnn_ok = ncnn_cmake.exists()
    opencv_ok = opencv_cmake.exists()

    print(f" NCNN arm64 CMake config : {'[EXISTS]' if ncnn_ok else '[MISSING]'} -> {ncnn_cmake}")
    print(f" OpenCV Mobile CMake config: {'[EXISTS]' if opencv_ok else '[MISSING]'} -> {opencv_cmake}")

    if ncnn_ok and opencv_ok:
        print("\nAll native C++ dependencies are in place! You can now build CMake.")
    else:
        print("\nWarning: Some files were not found at the expected paths. Check directory structures.")


if __name__ == "__main__":
    try:
        setup_ncnn()
        setup_opencv()
        verify_setup()
    except Exception as e:
        print(f"\n[ERROR] Setup failed: {e}")
        sys.exit(1)
