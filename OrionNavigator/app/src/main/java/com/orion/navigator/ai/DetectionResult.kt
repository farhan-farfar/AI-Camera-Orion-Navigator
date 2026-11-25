package com.orion.navigator.ai

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val classId: Int
)
