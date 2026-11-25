#include "yolo_detector.h"
#include <android/log.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <algorithm>
#include <cmath>

#define TAG "YoloDetector"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

YoloDetector::YoloDetector() {
    loaded = false;
    inputSize = 320;
}

YoloDetector::~YoloDetector() {
    release();
}

bool YoloDetector::loadModel(AAssetManager* assetManager, const char* modelDir) {
    if (!assetManager) {
        LOGE("AssetManager is null");
        return false;
    }

    std::string dir = modelDir ? modelDir : "models";
    std::string paramPath = dir + "/model.ncnn.param";
    std::string binPath = dir + "/model.ncnn.bin";

    LOGD("Loading model from: %s and %s", paramPath.c_str(), binPath.c_str());

    // Configure NCNN options for mobile efficiency & 4GB RAM devices
    net.opt = ncnn::Option();
    net.opt.use_vulkan_compute = false; // CPU inference by default for broadest compatibility
    net.opt.num_threads = 4;
    net.opt.use_packing_layout = true;
    net.opt.use_fp16_packed = true;
    net.opt.use_fp16_storage = true;
    net.opt.use_fp16_arithmetic = true;

    int retParam = net.load_param(assetManager, paramPath.c_str());
    if (retParam != 0) {
        LOGE("Failed to load param: %s (code %d)", paramPath.c_str(), retParam);
        loaded = false;
        return false;
    }

    int retBin = net.load_model(assetManager, binPath.c_str());
    if (retBin != 0) {
        LOGE("Failed to load bin: %s (code %d)", binPath.c_str(), retBin);
        loaded = false;
        return false;
    }

    loaded = true;
    LOGD("Model loaded successfully");
    return true;
}

cv::Mat YoloDetector::letterbox(const cv::Mat& src, int targetSize, float& scale, int& padW, int& padH) {
    int srcW = src.cols;
    int srcH = src.rows;

    scale = std::min((float)targetSize / (float)srcW, (float)targetSize / (float)srcH);
    int newW = (int)std::round((float)srcW * scale);
    int newH = (int)std::round((float)srcH * scale);

    cv::Mat resized;
    cv::resize(src, resized, cv::Size(newW, newH), 0, 0, cv::INTER_LINEAR);

    padW = (targetSize - newW) / 2;
    padH = (targetSize - newH) / 2;
    int padRight = targetSize - newW - padW;
    int padBottom = targetSize - newH - padH;

    cv::Mat boxed;
    cv::copyMakeBorder(
        resized,
        boxed,
        padH,
        padBottom,
        padW,
        padRight,
        cv::BORDER_CONSTANT,
        cv::Scalar(114, 114, 114)
    );

    return boxed;
}

std::vector<DetectionObject> YoloDetector::detect(const cv::Mat& rgbImage, float confThreshold, float nmsThreshold) {
    std::vector<DetectionObject> objects;
    if (!loaded || rgbImage.empty()) {
        return objects;
    }

    int srcW = rgbImage.cols;
    int srcH = rgbImage.rows;

    float scale = 1.0f;
    int padW = 0;
    int padH = 0;
    cv::Mat boxed = letterbox(rgbImage, inputSize, scale, padW, padH);

    // Convert OpenCV RGB Mat to NCNN Mat
    ncnn::Mat in = ncnn::Mat::from_pixels(
        boxed.data,
        ncnn::Mat::PIXEL_RGB,
        boxed.cols,
        boxed.rows
    );

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in.substract_mean_normalize(nullptr, norm_vals);

    ncnn::Extractor ex = net.create_extractor();
    ex.set_num_threads(4);

    // Try standard NCNN layer names for YOLO
    int retIn = ex.input("in0", in);
    if (retIn != 0) {
        ex.input("images", in);
    }

    ncnn::Mat out;
    int retOut = ex.extract("out0", out);
    if (retOut != 0) {
        ex.extract("output", out);
    }

    if (out.empty()) {
        LOGE("Inference failed: output blob is empty");
        return objects;
    }

    std::vector<DetectionObject> proposals;
    generateProposals(out, confThreshold, proposals, scale, padW, padH, srcW, srcH);
    nms(proposals, nmsThreshold);

    return proposals;
}

void YoloDetector::generateProposals(
    const ncnn::Mat& out,
    float confThreshold,
    std::vector<DetectionObject>& proposals,
    float scale,
    int padW,
    int padH,
    int srcW,
    int srcH
) {
    // YOLO raw output is 84 x N or N x 84
    const int numClasses = 80;

    if (out.h == 84) {
        // Shape: (num_candidates, 84) where out.w = num_candidates, out.h = 84
        int numCandidates = out.w;
        for (int i = 0; i < numCandidates; ++i) {
            float maxScore = 0.f;
            int maxClassId = -1;

            for (int c = 0; c < numClasses; ++c) {
                float score = out.row(4 + c)[i];
                if (score > maxScore) {
                    maxScore = score;
                    maxClassId = c;
                }
            }

            if (maxScore >= confThreshold) {
                float cx = out.row(0)[i];
                float cy = out.row(1)[i];
                float w = out.row(2)[i];
                float h = out.row(3)[i];

                float x1 = (cx - w / 2.f - (float)padW) / scale;
                float y1 = (cy - h / 2.f - (float)padH) / scale;
                float x2 = (cx + w / 2.f - (float)padW) / scale;
                float y2 = (cy + h / 2.f - (float)padH) / scale;

                // Clip to bounds
                x1 = std::max(0.0f, std::min(x1, (float)srcW));
                y1 = std::max(0.0f, std::min(y1, (float)srcH));
                x2 = std::max(0.0f, std::min(x2, (float)srcW));
                y2 = std::max(0.0f, std::min(y2, (float)srcH));

                if (x2 > x1 && y2 > y1) {
                    DetectionObject obj;
                    obj.x1 = x1;
                    obj.y1 = y1;
                    obj.x2 = x2;
                    obj.y2 = y2;
                    obj.confidence = maxScore;
                    obj.classId = maxClassId;
                    proposals.push_back(obj);
                }
            }
        }
    } else if (out.w == 84) {
        // Shape: (84, num_candidates) where out.h = num_candidates, out.w = 84
        int numCandidates = out.h;
        for (int i = 0; i < numCandidates; ++i) {
            const float* rowPtr = out.row(i);
            float maxScore = 0.f;
            int maxClassId = -1;

            for (int c = 0; c < numClasses; ++c) {
                float score = rowPtr[4 + c];
                if (score > maxScore) {
                    maxScore = score;
                    maxClassId = c;
                }
            }

            if (maxScore >= confThreshold) {
                float cx = rowPtr[0];
                float cy = rowPtr[1];
                float w = rowPtr[2];
                float h = rowPtr[3];

                float x1 = (cx - w / 2.f - (float)padW) / scale;
                float y1 = (cy - h / 2.f - (float)padH) / scale;
                float x2 = (cx + w / 2.f - (float)padW) / scale;
                float y2 = (cy + h / 2.f - (float)padH) / scale;

                x1 = std::max(0.0f, std::min(x1, (float)srcW));
                y1 = std::max(0.0f, std::min(y1, (float)srcH));
                x2 = std::max(0.0f, std::min(x2, (float)srcW));
                y2 = std::max(0.0f, std::min(y2, (float)srcH));

                if (x2 > x1 && y2 > y1) {
                    DetectionObject obj;
                    obj.x1 = x1;
                    obj.y1 = y1;
                    obj.x2 = x2;
                    obj.y2 = y2;
                    obj.confidence = maxScore;
                    obj.classId = maxClassId;
                    proposals.push_back(obj);
                }
            }
        }
    }
}

float YoloDetector::intersectionOverUnion(const DetectionObject& a, const DetectionObject& b) {
    float interX1 = std::max(a.x1, b.x1);
    float interY1 = std::max(a.y1, b.y1);
    float interX2 = std::min(a.x2, b.x2);
    float interY2 = std::min(a.y2, b.y2);

    float interW = std::max(0.0f, interX2 - interX1);
    float interH = std::max(0.0f, interY2 - interY1);
    float interArea = interW * interH;

    float areaA = (a.x2 - a.x1) * (a.y2 - a.y1);
    float areaB = (b.x2 - b.x1) * (b.y2 - b.y1);
    float unionArea = areaA + areaB - interArea;

    if (unionArea <= 0.0f) return 0.0f;
    return interArea / unionArea;
}

void YoloDetector::nms(std::vector<DetectionObject>& objects, float nmsThreshold) {
    std::sort(objects.begin(), objects.end(), [](const DetectionObject& a, const DetectionObject& b) {
        return a.confidence > b.confidence;
    });

    std::vector<DetectionObject> result;
    std::vector<bool> suppressed(objects.size(), false);

    for (size_t i = 0; i < objects.size(); ++i) {
        if (suppressed[i]) continue;
        result.push_back(objects[i]);

        for (size_t j = i + 1; j < objects.size(); ++j) {
            if (suppressed[j]) continue;
            // Class-specific NMS
            if (objects[i].classId == objects[j].classId) {
                float iou = intersectionOverUnion(objects[i], objects[j]);
                if (iou > nmsThreshold) {
                    suppressed[j] = true;
                }
            }
        }
    }

    objects = std::move(result);
}

void YoloDetector::release() {
    net.clear();
    loaded = false;
}

bool YoloDetector::isLoaded() const {
    return loaded;
}
