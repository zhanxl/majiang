package com.majiang.vision

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analysisCallback: ((Bitmap) -> Unit)? = null

    var isCameraRunning = false
        private set

    fun startCamera(
        surfaceProvider: Preview.SurfaceProvider,
        onAnalysisFrame: ((Bitmap) -> Unit)? = null
    ) {
        analysisCallback = onAnalysisFrame

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(cameraExecutor, { imageProxy ->
                        processImageProxy(imageProxy)
                    })
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                isCameraRunning = true
                Timber.d("Camera started successfully")
            } catch (e: Exception) {
                Timber.e(e, "Camera start failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        isCameraRunning = false
        Timber.d("Camera stopped")
    }

    suspend fun capturePhoto(outputFile: File): File {
        val capture = imageCapture ?: throw IllegalStateException("Camera not initialized")

        return suspendCancellableCoroutine { continuation ->
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

            capture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(outputFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )

            continuation.invokeOnCancellation {
                capture.cancelIntercept()
            }
        }
    }

    fun capturePhotoToBitmap(onResult: (Bitmap?) -> Unit) {
        val capture = imageCapture ?: run {
            onResult(null)
            return
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    onResult(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Photo capture failed")
                    onResult(null)
                }
            }
        )
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        analysisCallback?.invoke(bitmap)
        imageProxy.close()
    }

    fun release() {
        stopCamera()
        cameraExecutor.shutdownNow()
    }
}
