# Orion Navigator — AI Camera untuk Pengguna Tunanetra

**Orion Navigator** adalah aplikasi Android berbasis kecerdasan buatan (*AI Camera*) yang dirancang khusus dengan filosofi *accessibility-first* untuk membantu penyandang tunanetra dan *low vision* mengenali objek, memahami lingkungan visual sekitar, serta mendeteksi potensi rintangan di depan kamera smartphone secara mandiri melalui *Text-to-Speech* (TTS) bahasa Indonesia.

Aplikasi ini beroperasi **100% offline (on-device inference)** tanpa memerlukan server eksternal, koneksi internet, API backend, ataupun cloud computing.
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![C++](https://img.shields.io/badge/c%2B%2B-%2300599C.svg?style=for-the-badge&logo=cplusplus&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Python](https://img.shields.io/badge/python-%233670A0.svg?style=for-the-badge&logo=python&logoColor=ffdd54)
![YOLO](https://img.shields.io/badge/YOLO-%23111F68.svg?style=for-the-badge&logo=yolo&logoColor=white)
---

## Arsitektur Sistem

```
+-----------------------------------------------------------------------------------+
|                            ORION NAVIGATOR ARCHITECTURE                           |
+-----------------------------------------------------------------------------------+

       [ Pengguna Tunanetra ]
                 |
          (Tombol Capture / 
           TalkBack Action)
                 v
+------------------------------------+
|         ANDROID PRESENTATION       |
|  - MainActivity (Portrait, Clean)  |
|  - Material Design 3 High-Contrast |
|  - TalkBack LiveRegion ("polite")  |
+------------------------------------+
                 |
                 v
+------------------------------------+
|           CAMERAX LAYER            |
|  - CameraManager (Lifecycle-aware) |
|  - PreviewView (Live Preview)      |
|  - ImageCapture (Single Frame)     |
+------------------------------------+
                 | (Bitmap RGBA_8888)
                 v
+------------------------------------+
|          AI CORE INTEGRATION       |
|  - ObjectDetector (Kotlin)         |
|  - NativeInference (JNI Bridge)    |
|  - ImageProcessor (Letterbox/Scale)|
+------------------------------------+
                 | JNI
                 v
+-------------------------------------------------------------+
|               C++ / NCNN INFERENCE ENGINE                   |
|  - liborion_ncnn.so                                         |
|  - YoloDetector (C++)                                       |
|  - OpenCV Mobile (Letterbox RGB Preprocessing)              |
|  - Tencent NCNN (CPU Multi-threaded 4 Cores, FP16)          |
|  - Model YOLO26n / YOLO Nano (Input 320x320)                |
|  - Manual Post-Processing & Class-specific NMS              |
+-------------------------------------------------------------+
                 | Bounding Boxes + Classes (Flat Float Array)
                 v
+------------------------------------+
|          RESULT FORMATTER          |
|  - Mapping Kelas ke Bhs Indonesia  |
|  - Agregasi Jumlah Objek           |
|  - Konversi Kalimat Alami          |
+------------------------------------+
                 | Kalimat Deskriptif
                 v
+-------------------------------------------------------------+
|                     OUTPUT FEEDBACK                         |
|  - SpeechManager (Android Text-to-Speech - id_ID)           |
|  - HapticManager (Vibrator Waveform Feedback)               |
|  - High-Contrast Visual Cards (48dp+ Touch Targets)         |
+-------------------------------------------------------------+
```

---

## Fitur Utama

- **Accessibility-First Design**:
  - Palet warna kontras tinggi: Deep Navy (`#101828`), Cyan Accent (`#22D3EE`), Dark Background (`#0B1220`).
  - Target sentuh minimal 48dp (Tombol capture utama berukuran 80dp dengan haptic feedback).
  - Integrasi penuh dengan **Android TalkBack** melalui `contentDescription` dan `accessibilityLiveRegion="polite"`.
  - Fokus keyboard/switch yang terurut secara logis.
- **Kamera Cerdas Berbasis CameraX**:
  - Akses kamera belakang stabil dengan rotasi otomatis berdasarkan metadata EXIF.
  - Alur tangkap gambar asinkron non-blocking menggunakan Kotlin Coroutines.
- **On-Device Offline Inference (NCNN + C++/JNI)**:
  - Model object detection YOLO26n (resolusi input optimal $320 \times 320$).
  - Kompatibel dengan perangkat Android kelas menengah ke bawah (RAM mulai dari 4 GB).
  - Eksekusi pada background thread (Dispatchers.Default) agar UI tetap responsif.
- **Umpan Balik Suara Alami (Bahasa Indonesia)**:
  - Mengonversi hasil deteksi menjadi kalimat deskriptif singkat dan jelas.
  - Menghindari penyebutan confidence score secara teknis yang tidak relevan bagi pengguna.
  - Menyediakan tombol "ULANGI SUARA" dan "AMBIL FOTO LAGI".
  - Pembacaan suara otomatis dihentikan saat foto baru diambil untuk mencegah tumpang-tindih audio.

---

---

## Panduan Menjalankan Proyek

### A. Persiapan Lingkungan Pengembangan

1. **Android Studio**: Android Studio Koala / Ladybug atau versi terbaru.
2. **Android SDK**: API level 35, Build-tools 35.0.0+, Android NDK 28.2.13676358.
3. **Python**: Versi 3.10+ dengan pip.

### B. Persiapan Library Native (C++/NCNN)

Jalankan skrip otomatis dari direktori `ai-development/` untuk mengunduh library NCNN Android Vulkan dan OpenCV Mobile:

```bash
cd ai-development
pip install -r requirements.txt
python download_dependencies.py
```

Skrip ini akan menaruh file di:
- `OrionNavigator/app/src/main/cpp/ncnn-android-vulkan/`
- `OrionNavigator/app/src/main/cpp/opencv-mobile/`

### C. Persiapan & Ekspor Model AI

Ekspor model YOLO26n (atau YOLO nano pilihan Anda) ke format NCNN:

```bash
python export_ncnn.py --model yolo26n.pt --imgsz 320
```

File model `model.ncnn.param` dan `model.ncnn.bin` akan disalin secara otomatis ke direktori:
`OrionNavigator/app/src/main/assets/models/`

### D. Kompilasi & Menjalankan Aplikasi Android

1. Buka folder `OrionNavigator` di Android Studio.
2. Pastikan file `local.properties` telah mengarah ke Android SDK Anda.
3. Lakukan **Sync Project with Gradle Files**.
4. Hubungkan smartphone Android (disarankan dengan arsitektur `arm64-v8a` dan RAM minimal 4 GB).
5. Klik **Run 'app'** (`Shift + F10`).

---

## Optimasi Performa untuk Smartphone Entry-Level (RAM 4GB)

- **Input Resolution $320 \times 320$**: Menyeimbangkan rasio akurasi deteksi objek dengan beban komputasi CPU seluler.
- **Zero Memory Leaks pada Bitmap**: Bitmap hasil jepretan kamera langsung dibebaskan dari memori (`recycleSafely`) sesaat setelah inferensi selesai.
- **CPU Multi-threading**: NCNN dikonfigurasi menggunakan 4 thread CPU dengan kuantisasi FP16 bawaan (`use_fp16_packed`, `use_fp16_storage`), mencegah pemborosan daya dan panas berlebih.
- **Debounce / Single Execution**: Tombol capture langsung dinonaktifkan saat pemrosesan berlangsung, mencegah penumpukan tugas inferensi berulang.

---

## Penanganan Kondisi Error

| Skenario Error | Respon UI | Respon Audio (TTS) | Respon Haptic |
|---|---|---|---|
| Izin kamera ditolak | Status berubah menjelaskan izin | "Aplikasi ini membutuhkan akses kamera untuk mengenali objek. Silakan berikan izin kamera." | Getaran error |
| Model AI belum ada | Status menampilkan model tidak tersedia | "Model AI belum tersedia. Silakan pasang model terlebih dahulu." | Getaran error |
| Objek tidak terdeteksi | Menampilkan 0 objek terdeteksi | "Maaf, saya belum dapat mengenali objek pada gambar. Silakan coba mengambil gambar kembali." | Standar |
| Kegagalan inferensi / memori | Status: Gagal memproses gambar | "Gagal memproses gambar" | Getaran error |

---

## Lisensi

Hak Cipta © 2026 Tim Pengembang Orion Navigator. 
