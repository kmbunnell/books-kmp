@file:OptIn(ExperimentalForeignApi::class)

package com.example.books_kmp.ui.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.ui.permission.CameraSettingsDialog
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    val permissionGranted = remember { mutableStateOf(false) }
    val permissionDenied = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> permissionGranted.value = true
            AVAuthorizationStatusNotDetermined ->
                suspendCancellableCoroutine { cont ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        if (granted) permissionGranted.value = true else permissionDenied.value = true
                        cont.resume(Unit)
                    }
                }
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted -> permissionDenied.value = true
            else ->
                onResult(
                    Result.Failure(
                        BarcodeScanError.Unknown(
                            IllegalStateException("Unexpected AVAuthorizationStatus for video media type"),
                        ),
                    ),
                )
        }
    }

    when {
        permissionGranted.value -> CameraPreview(onResult = onResult)
        permissionDenied.value ->
            CameraSettingsDialog(
                onOpenSettings = {
                    val url = NSURL(string = UIApplicationOpenSettingsURLString) ?: return@CameraSettingsDialog
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any?>(),
                        completionHandler = null,
                    )
                },
                onDismiss = { onResult(Result.Failure(BarcodeScanError.CameraPermissionDenied)) },
            )
    }
}

@Composable
private fun CameraPreview(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    val onResultState = rememberUpdatedState(onResult)
    // One-shot delivery guard — both paths (delegate callback, dispose) run on the main thread.
    val hasDelivered = remember { DeliveryGuard() }
    val sessionRef = remember { mutableStateOf<AVCaptureSession?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            if (hasDelivered.tryDeliver()) {
                onResultState.value(Result.Failure(BarcodeScanError.Cancelled))
            }
            sessionRef.value?.stopRunning()
        }
    }

    UIKitView(
        factory = {
            val session = AVCaptureSession()
            sessionRef.value = session
            BarcodeScanView(session = session, hasDelivered = hasDelivered, onResultState = onResultState)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// All access is on the main thread: delegate queue is main, DisposableEffect runs on main.
private class DeliveryGuard {
    private var delivered = false

    fun tryDeliver(): Boolean {
        if (delivered) return false
        delivered = true
        return true
    }
}

private class BarcodeScanView(
    session: AVCaptureSession,
    hasDelivered: DeliveryGuard,
    onResultState: State<(Result<String, BarcodeScanError>) -> Unit>,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    // Strong reference prevents ARC from releasing the delegate before scanning completes.
    @Suppress("unused")
    private var barcodeDelegate: BarcodeMetadataDelegate? = null

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
        setupSession(session, hasDelivered, onResultState)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.setFrame(bounds)
    }

    private fun setupSession(
        session: AVCaptureSession,
        hasDelivered: DeliveryGuard,
        onResultState: State<(Result<String, BarcodeScanError>) -> Unit>,
    ) {
        try {
            val device =
                AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                    ?: error("No video capture device found")
            val input =
                AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                    ?: error("Could not create AVCaptureDeviceInput")
            if (session.canAddInput(input)) session.addInput(input)
            val metadataOutput = AVCaptureMetadataOutput()
            if (session.canAddOutput(metadataOutput)) session.addOutput(metadataOutput)
            val delegate = BarcodeMetadataDelegate(session, hasDelivered, onResultState)
            barcodeDelegate = delegate
            metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes =
                listOf(AVMetadataObjectTypeEAN13Code)
            dispatch_async(dispatch_get_global_queue(0L, 0u)) {
                session.startRunning()
            }
        } catch (e: Throwable) {
            if (hasDelivered.tryDeliver()) {
                onResultState.value(Result.Failure(BarcodeScanError.Unknown(e)))
            }
        }
    }
}

private class BarcodeMetadataDelegate(
    private val session: AVCaptureSession,
    private val hasDelivered: DeliveryGuard,
    private val onResultState: State<(Result<String, BarcodeScanError>) -> Unit>,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val obj = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject ?: return
        val raw = obj.stringValue ?: return
        if (hasDelivered.tryDeliver()) {
            onResultState.value(Result.Success(raw))
            dispatch_async(dispatch_get_global_queue(0L, 0u)) {
                session.stopRunning()
            }
        }
    }
}
