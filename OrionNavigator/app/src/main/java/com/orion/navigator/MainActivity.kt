package com.orion.navigator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.orion.navigator.accessibility.HapticManager
import com.orion.navigator.accessibility.SpeechManager
import com.orion.navigator.ai.DetectionResult
import com.orion.navigator.ai.ObjectDetector
import com.orion.navigator.camera.CameraManager
import com.orion.navigator.databinding.ActivityMainBinding
import com.orion.navigator.utils.ImageProcessor
import com.orion.navigator.utils.ResultFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var objectDetector: ObjectDetector
    private lateinit var speechManager: SpeechManager
    private lateinit var hapticManager: HapticManager

    private var isAnalyzing = false
    private var lastResultText = ""
    private var isCameraStarted = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            handlePermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initManagers()
        setupListeners()
        checkCameraPermission()
    }

    private fun initManagers() {
        speechManager = SpeechManager().apply {
            onInitComplete = {
                if (isTtsReady) {
                    speak(getString(R.string.tts_welcome))
                }
            }
            init(this@MainActivity)
        }

        hapticManager = HapticManager().apply {
            init(this@MainActivity)
        }

        cameraManager = CameraManager(this)
        objectDetector = ObjectDetector()

        // Initialize AI model in background
        lifecycleScope.launch(Dispatchers.IO) {
            if (ObjectDetector.isModelAvailable(this@MainActivity)) {
                val loaded = objectDetector.init(assets)
                Log.d(TAG, "AI model initialization status: $loaded")
            } else {
                Log.w(TAG, "AI model files not found in assets/models")
            }
        }
    }

    private fun setupListeners() {
        binding.btnCapture.setOnClickListener {
            onCaptureClicked()
        }

        binding.btnRepeatVoice.setOnClickListener {
            hapticManager.vibrateClick()
            if (lastResultText.isNotEmpty()) {
                speechManager.speak(lastResultText)
            } else {
                speechManager.speak(getString(R.string.tts_no_object))
            }
        }

        binding.btnRetake.setOnClickListener {
            hapticManager.vibrateClick()
            resetToCameraReady()
        }

        binding.btnSettings.setOnClickListener {
            hapticManager.vibrateClick()
            val info = "Orion Navigator versi 1.0. Aplikasi kamera kecerdasan buatan untuk membantu tunanetra mengenali objek."
            speechManager.speak(info)
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                setupCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                speechManager.speak(getString(R.string.tts_permission_needed))
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupCamera() {
        if (isCameraStarted) return
        isCameraStarted = true

        cameraManager.startCamera(
            lifecycleOwner = this,
            previewView = binding.previewView,
            onSuccess = {
                binding.tvStatus.text = getString(R.string.status_camera_ready)
            },
            onError = { error ->
                Log.e(TAG, "Camera start error", error)
                binding.tvStatus.text = getString(R.string.tts_camera_error)
                speechManager.speak(getString(R.string.tts_camera_error))
                hapticManager.vibrateError()
            }
        )
    }

    private fun handlePermissionDenied() {
        val message = getString(R.string.tts_permission_needed)
        binding.tvStatus.text = message
        speechManager.speak(message)
        hapticManager.vibrateError()
    }

    private fun onCaptureClicked() {
        if (isAnalyzing) return

        hapticManager.vibrateClick()
        setAnalyzingState(true)

        lifecycleScope.launch {
            var capturedBitmap: Bitmap? = null
            try {
                binding.tvStatus.text = getString(R.string.status_capturing)

                capturedBitmap = cameraManager.captureImage()

                binding.tvStatus.text = getString(R.string.status_analyzing)
                speechManager.speak(getString(R.string.tts_analyzing))

                // Perform inference
                val results: List<DetectionResult> = withContext(Dispatchers.Default) {
                    if (!ObjectDetector.isModelAvailable(this@MainActivity) || !objectDetector.isNativeLibraryLoaded()) {
                        null
                    } else {
                        objectDetector.detect(capturedBitmap)
                    }
                } ?: run {
                    handleModelNotAvailable()
                    return@launch
                }

                // Process results
                handleDetectionSuccess(results)

            } catch (e: Exception) {
                Log.e(TAG, "Detection flow error", e)
                handleDetectionError(e)
            } finally {
                capturedBitmap?.let { ImageProcessor.recycleSafely(it) }
                setAnalyzingState(false)
            }
        }
    }

    private fun handleDetectionSuccess(results: List<DetectionResult>) {
        binding.tvStatus.text = getString(R.string.status_done)
        val formattedSentence = ResultFormatter.formatResults(results)
        lastResultText = formattedSentence

        // Update UI
        binding.tvResultContent.text = formattedSentence
        binding.tvObjectCount.text = if (results.isNotEmpty()) {
            getString(R.string.result_object_count, results.size, "objek terdeteksi")
        } else {
            "0 objek terdeteksi"
        }

        binding.resultSection.visibility = View.VISIBLE
        binding.actionButtons.visibility = View.VISIBLE

        hapticManager.vibrateSuccess()
        speechManager.speak(formattedSentence)
    }

    private fun handleModelNotAvailable() {
        val errorText = getString(R.string.tts_model_missing)
        binding.tvStatus.text = errorText
        speechManager.speak(errorText)
        hapticManager.vibrateError()

        binding.tvResultContent.text = errorText
        binding.tvObjectCount.text = "0 objek terdeteksi"
        binding.resultSection.visibility = View.VISIBLE
        binding.actionButtons.visibility = View.VISIBLE
    }

    private fun handleDetectionError(exception: Exception) {
        val errorText = getString(R.string.status_error)
        binding.tvStatus.text = errorText
        speechManager.speak(errorText)
        hapticManager.vibrateError()
    }

    private fun resetToCameraReady() {
        binding.resultSection.visibility = View.GONE
        binding.actionButtons.visibility = View.GONE
        binding.tvStatus.text = getString(R.string.status_camera_ready)
        speechManager.speak(getString(R.string.status_camera_ready))
        lastResultText = ""
    }

    private fun setAnalyzingState(analyzing: Boolean) {
        isAnalyzing = analyzing
        binding.btnCapture.isEnabled = !analyzing
        binding.btnCapture.alpha = if (analyzing) 0.5f else 1.0f
    }

    override fun onStop() {
        super.onStop()
        speechManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.release()
        cameraManager.release()
        objectDetector.release()
    }
}
