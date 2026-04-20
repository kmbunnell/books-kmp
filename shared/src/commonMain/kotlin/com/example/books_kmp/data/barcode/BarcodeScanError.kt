package com.example.books_kmp.data.barcode

sealed interface BarcodeScanError {
    data object Cancelled : BarcodeScanError

    data object CameraPermissionDenied : BarcodeScanError

    data object HardwareUnavailable : BarcodeScanError

    data class Unknown(val cause: Throwable?) : BarcodeScanError
}
