cmake_minimum_required(VERSION 3.22.1)
project(orion_ncnn)

set(CMAKE_CXX_STANDARD 17)

# ==============================================================================
# Orion Navigator - NCNN Native Build Configuration
# ==============================================================================
#
# Expected directory structure under app/src/main/cpp/:
#
#   cpp/
#   ├── CMakeLists.txt              (this file)
#   ├── orion_jni.cpp               (JNI bridge)
#   ├── yolo_detector.h             (YOLO detector header)
#   ├── yolo_detector.cpp           (YOLO detector implementation)
#   ├── ncnn-android-vulkan/        (NCNN library - download required)
#   │   ├── arm64-v8a/
#   │   │   ├── include/
#   │   │   │   └── ncnn/
#   │   │   └── lib/
#   │   │       ├── cmake/ncnn/
#   │   │       └── libncnn.a
#   │   └── ...
#   └── opencv-mobile/             (OpenCV Mobile - download required)
#       └── sdk/
#           └── native/
#               └── jni/
#                   ├── OpenCVConfig.cmake
#                   └── include/
#                       └── opencv2/
#
# ==============================================================================
# SETUP INSTRUCTIONS:
#
# 1. Download NCNN Android Vulkan prebuilt package:
#    https://github.com/Tencent/ncnn/releases
#    - Download: ncnn-YYYYMMDD-android-vulkan.zip
#    - Extract to: app/src/main/cpp/ncnn-android-vulkan/
#
# 2. Download OpenCV Mobile for Android:
#    https://github.com/nicegram/opencv-mobile/releases
#    - Download: opencv-mobile-X.X.X-android.zip
#    - Extract to: app/src/main/cpp/opencv-mobile/
#
# ==============================================================================

# --- NCNN Configuration ---
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)

if(NOT EXISTS "${ncnn_DIR}")
    message(FATAL_ERROR
        "\n"
        "========================================================================\n"
        " NCNN library not found!\n"
        "========================================================================\n"
        " Expected path: ${ncnn_DIR}\n"
        "\n"
        " Please download the NCNN Android Vulkan package from:\n"
        "   https://github.com/Tencent/ncnn/releases\n"
        "\n"
        " Then extract it to:\n"
        "   app/src/main/cpp/ncnn-android-vulkan/\n"
        "\n"
        " The directory should contain ABI folders like arm64-v8a/\n"
        "========================================================================\n"
    )
endif()

find_package(ncnn REQUIRED)

# --- OpenCV Mobile Configuration ---
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv-mobile/sdk/native/jni)

if(NOT EXISTS "${OpenCV_DIR}")
    message(FATAL_ERROR
        "\n"
        "========================================================================\n"
        " OpenCV Mobile not found!\n"
        "========================================================================\n"
        " Expected path: ${OpenCV_DIR}\n"
        "\n"
        " Please download OpenCV Mobile for Android from:\n"
        "   https://github.com/nicegram/opencv-mobile/releases\n"
        "\n"
        " Then extract it to:\n"
        "   app/src/main/cpp/opencv-mobile/\n"
        "\n"
        " The directory should contain sdk/native/jni/OpenCVConfig.cmake\n"
        "========================================================================\n"
    )
endif()

find_package(OpenCV REQUIRED)

# --- Orion NCNN Shared Library ---
add_library(orion_ncnn SHARED
    orion_jni.cpp
    yolo_detector.cpp
)

target_include_directories(orion_ncnn PRIVATE
    ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/include
    ${CMAKE_SOURCE_DIR}/opencv-mobile/sdk/native/jni/include
)

target_link_libraries(orion_ncnn
    ncnn
    ${OpenCV_LIBS}
    android
    jnigraphics
    log
)
