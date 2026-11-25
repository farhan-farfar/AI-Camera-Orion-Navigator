package com.orion.navigator.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager {

    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateClick() {
        vibrate(50L)
    }

    fun vibrateSuccess() {
        val timings = longArrayOf(0, 80, 100, 80, 100, 120)
        val amplitudes = intArrayOf(0, 120, 0, 150, 0, 200)
        vibratePattern(timings, amplitudes)
    }

    fun vibrateError() {
        val timings = longArrayOf(0, 200, 100, 200)
        val amplitudes = intArrayOf(0, 255, 0, 255)
        vibratePattern(timings, amplitudes)
    }

    private fun vibrate(durationMs: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        v.vibrate(effect)
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        v.vibrate(effect)
    }
}
