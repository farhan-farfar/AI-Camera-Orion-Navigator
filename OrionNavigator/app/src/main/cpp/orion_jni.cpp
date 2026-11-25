#include <jni.h>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include <memory>
#include "yolo_detector.h"

#define TAG "OrionNcnnJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<YoloDetector> g_detector = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_orion_navigator_ai_NativeInference_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jobject assetManager,
    jstring modelDir
) {
    if (!assetManager) {
        LOGE("nativeInit: AssetManager is null");
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!mgr) {
        LOGE("nativeInit: Failed to retrieve AAssetManager");
        return JNI_FALSE;
    }

    const char* dirStr = nullptr;
    if (modelDir) {
        dirStr = env->GetStringUTFChars(modelDir, nullptr);
    }

    if (!g_detector) {
        g_detector = std::make_unique<YoloDetector>();
    }

    bool success = g_detector->loadModel(mgr, dirStr ? dirStr : "models");

    if (dirStr) {
        env->ReleaseStringUTFChars(modelDir, dirStr);
    }

    LOGD("nativeInit result: %s", success ? "SUCCESS" : "FAILED");
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloatArray JNICALL
Java_com_orion_navigator_ai_NativeInference_nativeDetect(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jint width,
    jint height,
    jfloat threshold
) {
    if (!g_detector || !g_detector->isLoaded()) {
        LOGE("nativeDetect: Detector is not initialized");
        return nullptr;
    }

    if (!bitmap) {
        LOGE("nativeDetect: Bitmap is null");
        return nullptr;
    }

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("nativeDetect: Failed to get Bitmap info");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("nativeDetect: Bitmap format must be RGBA_8888, got: %d", info.format);
        return nullptr;
    }

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0 || !pixels) {
        LOGE("nativeDetect: Failed to lock Bitmap pixels");
        return nullptr;
    }

    cv::Mat rgba(info.height, info.width, CV_8UC4, pixels);
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);

    AndroidBitmap_unlockPixels(env, bitmap);

    std::vector<DetectionObject> objects = g_detector->detect(rgb, threshold);

    if (objects.empty()) {
        return nullptr;
    }

    // Pack into flat float array: [x1, y1, x2, y2, conf, classId, ...]
    const int fieldsPerObject = 6;
    std::vector<float> flatData;
    flatData.reserve(objects.size() * fieldsPerObject);

    for (const auto& obj : objects) {
        flatData.push_back(obj.x1);
        flatData.push_back(obj.y1);
        flatData.push_back(obj.x2);
        flatData.push_back(obj.y2);
        flatData.push_back(obj.confidence);
        flatData.push_back(static_cast<float>(obj.classId));
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(flatData.size()));
    if (!result) {
        LOGE("nativeDetect: Failed to allocate float array");
        return nullptr;
    }

    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(flatData.size()), flatData.data());
    return result;
}

JNIEXPORT void JNICALL
Java_com_orion_navigator_ai_NativeInference_nativeRelease(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    LOGD("nativeRelease called");
    if (g_detector) {
        g_detector->release();
        g_detector.reset();
    }
}

} // extern "C"
