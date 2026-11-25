package com.orion.navigator.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SpeechManager"
    }

    private var tts: TextToSpeech? = null
    var isTtsReady: Boolean = false
        private set
    var onInitComplete: (() -> Unit)? = null

    fun init(context: Context) {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val indonesian = Locale("id", "ID")
            val result = tts?.setLanguage(indonesian)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isTtsReady) {
                Log.w(TAG, "Indonesian TTS not available, falling back to default")
                tts?.setLanguage(Locale.getDefault())
                isTtsReady = true
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
            isTtsReady = false
        }
        onInitComplete?.invoke()
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isTtsReady || tts == null) {
            Log.w(TAG, "TTS not ready, cannot speak: $text")
            return
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "orion_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        onInitComplete = null
    }
}
