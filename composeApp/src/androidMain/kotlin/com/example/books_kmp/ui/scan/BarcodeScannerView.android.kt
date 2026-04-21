package com.example.books_kmp.ui.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.ui.permission.CameraPermissionState
import com.example.books_kmp.ui.permission.CameraRationaleDialog
import com.example.books_kmp.ui.permission.CameraSettingsDialog
import com.example.books_kmp.ui.permission.rememberCameraPermissionHandler
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    val permissionHandler = rememberCameraPermissionHandler()

    LaunchedEffect(Unit) {
        permissionHandler.request()
    }

    when (permissionHandler.state) {
        CameraPermissionState.Idle -> Unit
        CameraPermissionState.Granted -> CameraPreview(onResult = onResult)
        CameraPermissionState.Denied ->
            CameraRationaleDialog(
                onRetry = permissionHandler.request,
                onDismiss = { onResult(Result.Failure(BarcodeScanError.CameraPermissionDenied)) },
            )
        CameraPermissionState.PermanentlyDenied ->
            CameraSettingsDialog(
                onOpenSettings = permissionHandler.openSettings,
                onDismiss = { onResult(Result.Failure(BarcodeScanError.CameraPermissionDenied)) },
            )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val onResultState = rememberUpdatedState(onResult)
    val hasDelivered = remember { AtomicBoolean(false) }
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderState.value?.unbindAll()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener(
                {
                    try {
                        val provider = future.get().also { cameraProviderState.value = it }
                        val preview =
                            Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                        val client =
                            BarcodeScanning.getClient(
                                BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A)
                                    .build(),
                            )
                        val analysis =
                            ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { ia ->
                                    ia.setAnalyzer(executor) { imageProxy ->
                                        processFrame(imageProxy, client) { result ->
                                            if (hasDelivered.compareAndSet(false, true)) {
                                                onResultState.value(result)
                                            }
                                        }
                                    }
                                }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    } catch (e: Exception) {
                        if (hasDelivered.compareAndSet(false, true)) {
                            onResultState.value(Result.Failure(BarcodeScanError.Unknown(e)))
                        }
                    }
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processFrame(
    imageProxy: ImageProxy,
    client: com.google.mlkit.vision.barcode.BarcodeScanner,
    deliver: (Result<String, BarcodeScanError>) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    client
        .process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val raw = barcode.rawValue ?: continue
                deliver(Result.Success(raw))
                break
            }
        }
        .addOnFailureListener { e -> deliver(Result.Failure(BarcodeScanError.Unknown(e))) }
        .addOnCompleteListener { imageProxy.close() }
}
