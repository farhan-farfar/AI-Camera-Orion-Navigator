package com.orion.navigator.ai

import android.content.res.AssetManager
import android.graphics.Bitmap

object NativeInference {
    external fun nativeInit(assetManager: AssetManager, modelDir: String): Boolean
    external fun nativeDetect(bitmap: Bitmap, width: Int, height: Int, threshold: Float): FloatArray?
    external fun nativeRelease()
}
