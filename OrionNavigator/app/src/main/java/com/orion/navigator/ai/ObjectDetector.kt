package com.orion.navigator.ai

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import com.orion.navigator.utils.ImageProcessor

class ObjectDetector {

    companion object {
        private const val MODEL_DIR = "models"
        private const val MODEL_PARAM = "model.ncnn.param"
        private const val MODEL_BIN = "model.ncnn.bin"
        private const val INPUT_SIZE = 320

        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("orion_ncnn")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                isLibraryLoaded = false
            }
        }

        val COCO_CLASSES = arrayOf(
            "person", "bicycle", "car", "motorcycle", "airplane",
            "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird",
            "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat",
            "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon",
            "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut",
            "cake", "chair", "couch", "potted plant", "bed",
            "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven",
            "toaster", "sink", "refrigerator", "book", "clock",
            "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        )

        val INDONESIAN_NAMES = mapOf(
            "person" to "manusia",
            "bicycle" to "sepeda",
            "car" to "mobil",
            "motorcycle" to "sepeda motor",
            "bus" to "bus",
            "truck" to "truk",
            "traffic light" to "lampu lalu lintas",
            "fire hydrant" to "hidran",
            "stop sign" to "rambu berhenti",
            "bench" to "bangku",
            "cat" to "kucing",
            "dog" to "anjing",
            "backpack" to "tas ransel",
            "umbrella" to "payung",
            "handbag" to "tas tangan",
            "suitcase" to "koper",
            "bottle" to "botol",
            "cup" to "gelas",
            "fork" to "garpu",
            "knife" to "pisau",
            "spoon" to "sendok",
            "bowl" to "mangkuk",
            "chair" to "kursi",
            "couch" to "sofa",
            "potted plant" to "tanaman pot",
            "bed" to "tempat tidur",
            "dining table" to "meja makan",
            "toilet" to "toilet",
            "tv" to "televisi",
            "laptop" to "laptop",
            "cell phone" to "ponsel",
            "book" to "buku",
            "clock" to "jam",
            "scissors" to "gunting",
            "toothbrush" to "sikat gigi",
            "door" to "pintu"
        )

        fun getIndonesianName(englishName: String): String {
            return INDONESIAN_NAMES[englishName] ?: englishName
        }

        fun isNativeLibraryLoaded(): Boolean = isLibraryLoaded

        fun isModelAvailable(context: Context): Boolean {
            return try {
                val assets = context.assets.list(MODEL_DIR) ?: return false
                assets.contains(MODEL_PARAM) && assets.contains(MODEL_BIN)
            } catch (e: Exception) {
                false
            }
        }
    }

    private var isInitialized = false

    fun isNativeLibraryLoaded(): Boolean = isLibraryLoaded

    fun init(assetManager: AssetManager): Boolean {
        if (isInitialized) return true
        return try {
            isInitialized = NativeInference.nativeInit(assetManager, MODEL_DIR)
            isInitialized
        } catch (e: Exception) {
            isInitialized = false
            false
        }
    }

    fun detect(bitmap: Bitmap, confidenceThreshold: Float = 0.45f): List<DetectionResult> {
        if (!isInitialized) return emptyList()

        val resized = ImageProcessor.resizeBitmap(bitmap, INPUT_SIZE, INPUT_SIZE)
        return try {
            val rawResults = NativeInference.nativeDetect(
                resized,
                INPUT_SIZE,
                INPUT_SIZE,
                confidenceThreshold
            ) ?: return emptyList()

            parseResults(rawResults, bitmap.width, bitmap.height)
                .sortedByDescending { it.confidence }
        } finally {
            if (resized != bitmap) {
                ImageProcessor.recycleSafely(resized)
            }
        }
    }

    private fun parseResults(
        rawResults: FloatArray,
        originalWidth: Int,
        originalHeight: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val numDetections = rawResults.size / 6

        for (i in 0 until numDetections) {
            val offset = i * 6
            val x1 = rawResults[offset]
            val y1 = rawResults[offset + 1]
            val x2 = rawResults[offset + 2]
            val y2 = rawResults[offset + 3]
            val confidence = rawResults[offset + 4]
            val classId = rawResults[offset + 5].toInt()

            if (classId < 0 || classId >= COCO_CLASSES.size) continue

            val scaleX = originalWidth.toFloat() / INPUT_SIZE
            val scaleY = originalHeight.toFloat() / INPUT_SIZE

            val boundingBox = RectF(
                x1 * scaleX,
                y1 * scaleY,
                x2 * scaleX,
                y2 * scaleY
            )

            val label = COCO_CLASSES[classId]
            results.add(DetectionResult(label, confidence, boundingBox, classId))
        }

        return results
    }

    fun release() {
        if (isInitialized) {
            try {
                NativeInference.nativeRelease()
            } catch (_: Exception) {
            }
            isInitialized = false
        }
    }
}
