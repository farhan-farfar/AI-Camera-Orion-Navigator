package com.orion.navigator.utils

import com.orion.navigator.ai.DetectionResult
import com.orion.navigator.ai.ObjectDetector

object ResultFormatter {

    private val INDONESIAN_NUMBERS = arrayOf(
        "", "satu", "dua", "tiga", "empat", "lima",
        "enam", "tujuh", "delapan", "sembilan", "sepuluh"
    )

    fun formatResults(results: List<DetectionResult>): String {
        if (results.isEmpty()) {
            return "Maaf, saya belum dapat mengenali objek pada gambar. Silakan coba mengambil gambar kembali."
        }

        if (results.size == 1) {
            val name = ObjectDetector.getIndonesianName(results[0].label)
            return "Saya mendeteksi sebuah $name di depan kamera."
        }

        // Group by label
        val grouped = results.groupBy { it.label }
        val totalCount = results.size

        val parts = mutableListOf<String>()
        val entries = grouped.entries.toList()

        for ((index, entry) in entries.withIndex()) {
            val name = ObjectDetector.getIndonesianName(entry.key)
            val count = entry.value.size
            val numberStr = toIndonesianNumber(count)
            parts.add("$numberStr $name")
        }

        val listing = when (parts.size) {
            1 -> parts[0]
            2 -> "${parts[0]} dan ${parts[1]}"
            else -> {
                val allButLast = parts.dropLast(1).joinToString(", ")
                "$allButLast, dan ${parts.last()}"
            }
        }

        return "Saya mendeteksi $totalCount objek. Terdapat $listing."
    }

    private fun toIndonesianNumber(count: Int): String {
        return if (count in 1..10) {
            INDONESIAN_NUMBERS[count]
        } else {
            count.toString()
        }
    }
}
