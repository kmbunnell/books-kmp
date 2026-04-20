package com.example.books_kmp.data.barcode

import com.example.books_kmp.domain.Result
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

actual class BarcodeScanner {
    actual suspend fun scanBarcode(): Result<String, BarcodeScanError> {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            // TODO: launch scan UI once scanner is implemented
            AVAuthorizationStatusAuthorized -> Result.Failure(BarcodeScanError.Cancelled)
            AVAuthorizationStatusNotDetermined ->
                suspendCancellableCoroutine { cont ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        if (granted) {
                            cont.resume(Result.Failure(BarcodeScanError.Cancelled))
                        } else {
                            cont.resume(Result.Failure(BarcodeScanError.CameraPermissionDenied))
                        }
                    }
                }
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted -> Result.Failure(BarcodeScanError.CameraPermissionDenied)
            else -> Result.Failure(BarcodeScanError.Unknown(IllegalStateException("Unexpected AVAuthorizationStatus for video media type")))
        }
    }
}
