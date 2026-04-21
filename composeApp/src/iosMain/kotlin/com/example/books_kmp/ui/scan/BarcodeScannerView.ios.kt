package com.example.books_kmp.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BarcodeScanError
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import kotlin.coroutines.resume

// TODO: replace the Cancelled stub with a UIKitView + AVFoundation scanner once iOS scanning is implemented
@Composable
actual fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    LaunchedEffect(Unit) {
        val result =
            when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                AVAuthorizationStatusAuthorized -> Result.Failure(BarcodeScanError.Cancelled)
                AVAuthorizationStatusNotDetermined ->
                    suspendCancellableCoroutine { cont ->
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            cont.resume(
                                if (granted) Result.Failure(BarcodeScanError.Cancelled)
                                else Result.Failure(BarcodeScanError.CameraPermissionDenied),
                            )
                        }
                    }
                AVAuthorizationStatusDenied,
                AVAuthorizationStatusRestricted -> Result.Failure(BarcodeScanError.CameraPermissionDenied)
                else ->
                    Result.Failure(
                        BarcodeScanError.Unknown(
                            IllegalStateException("Unexpected AVAuthorizationStatus for video media type"),
                        ),
                    )
            }
        onResult(result)
    }
}
