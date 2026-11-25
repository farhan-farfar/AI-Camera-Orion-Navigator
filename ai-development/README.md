# Orion Navigator - AI Model Development & Export Guide

Modul ini bertanggung jawab untuk pengujian model object detection YOLO (kandidat awal **YOLO26n**), evaluasi akurasi, dan ekspor ke format **NCNN** agar dapat dijalankan secara *on-device* dan *offline* pada smartphone Android melalui C++/JNI.

---

## 1. Persyaratan Sistem (Prerequisites)

- Python 3.10 atau lebih baru
- Virtual environment (direkomendasikan)
- Pip & Git

---

## 2. Instalasi Dependency

Buka terminal di direktori `ai-development/` dan jalankan:

```bash
# Buat virtual environment (opsional)
python -m venv venv
# Windows
venv\Scripts\activate
# Linux/macOS
source venv/bin/activate

# Instal seluruh dependency
pip install -r requirements.txt
```

---

## 3. Menjalankan Pengujian Model (Inference Test)

Gunakan skrip `test_model.py` untuk menguji model pada gambar uji dan melihat simulasi keluaran Text-to-Speech (TTS) bahasa Indonesia:

```bash
python test_model.py --model yolo26n.pt --image path/ke/gambar_uji.jpg --conf 0.45
```

**Output yang dihasilkan mencakup:**
- Daftar objek yang terdeteksi (kelas, confidence, koordinat bounding box).
- Terjemahan nama objek ke Bahasa Indonesia.
- Kalimat alami simulasi Text-to-Speech (TTS) sesuai standar aksesibilitas Orion Navigator, misalnya:
  > *"Saya mendeteksi sebuah kursi di depan kamera."*
  > atau
  > *"Saya mendeteksi tiga objek. Terdapat satu kursi, satu meja, dan satu botol."*

---

## 4. Ekspor Model ke Format NCNN

Skrip `export_ncnn.py` mengonversi bobot model YOLO ke format NCNN (`.param` dan `.bin`) dengan resolusi input default $320 \times 320$:

```bash
python export_ncnn.py --model yolo26n.pt --imgsz 320
```

Skrip ini akan secara otomatis:
1. Mengekspor model menggunakan Ultralytics NCNN converter.
2. Memverifikasi integritas file `model.ncnn.param` dan `model.ncnn.bin`.
3. Menyalin kedua file tersebut langsung ke direktori asset aplikasi Android:
   `OrionNavigator/app/src/main/assets/models/`

---

## 5. Mengunduh Library Native NCNN & OpenCV Mobile untuk Android NDK

Untuk mengompilasi shared library `liborion_ncnn.so` melalui CMake di Android Studio, Anda membutuhkan prebuilt binary NCNN Android Vulkan dan OpenCV Mobile. Jalankan skrip otomatis berikut:

```bash
python download_dependencies.py
```

Skrip akan mengunduh dan menempatkan paket ke direktori:
- `OrionNavigator/app/src/main/cpp/ncnn-android-vulkan/`
- `OrionNavigator/app/src/main/cpp/opencv-mobile/`

---

## 6. Struktur Output Model di Android Assets

Setelah diekspor, direktori aset Android harus memiliki struktur berikut:

```text
OrionNavigator/app/src/main/assets/
└── models/
    ├── model.ncnn.param   # Arsitektur graf jaringan NCNN
    └── model.ncnn.bin     # Bobot/weights model terkuantisasi
```
