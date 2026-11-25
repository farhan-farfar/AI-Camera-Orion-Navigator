#pragma once

#include <vector>
#include <string>
#include <android/asset_manager.h>
#include <opencv2/core/core.hpp>
#include <ncnn/net.h>

struct DetectionObject {
    float x1;
    float y1;
    float x2;
    float y2;
    float confidence;
    int classId;
};

class YoloDetector {
public:
    YoloDetector();
    ~YoloDetector();

    bool loadModel(AAssetManager* assetManager, const char* modelDir);
    std::vector<DetectionObject> detect(const cv::Mat& rgbImage, float confThreshold = 0.45f, float nmsThreshold = 0.5f);
    void release();
    bool isLoaded() const;

private:
    ncnn::Net net;
    bool loaded = false;
    int inputSize = 320; // Target input size for YOLO26n / mobile inference

    // Preprocessing: Letterboxing to maintain aspect ratio with gray padding
    cv::Mat letterbox(const cv::Mat& src, int targetSize, float& scale, int& padW, int& padH);

    // Postprocessing: Decoding raw YOLO (1, 84, 8400) output tensor
    void generateProposals(
        const ncnn::Mat& out,
        float confThreshold,
        std::vector<DetectionObject>& proposals,
        float scale,
        int padW,
        int padH,
        int srcW,
        int srcH
    );

    // Non-Maximum Suppression
    void nms(std::vector<DetectionObject>& objects, float nmsThreshold);
    float intersectionOverUnion(const DetectionObject& a, const DetectionObject& b);
};
