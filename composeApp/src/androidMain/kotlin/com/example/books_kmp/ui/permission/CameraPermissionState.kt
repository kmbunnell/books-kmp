package com.example.books_kmp.ui.permission

sealed interface CameraPermissionState {
    data object Idle : CameraPermissionState

    data object Granted : CameraPermissionState

    data object Denied : CameraPermissionState

    data object PermanentlyDenied : CameraPermissionState

    companion object {
        fun fromResult(
            granted: Boolean,
            shouldShowRationale: Boolean
        ): CameraPermissionState =
            when {
                granted -> Granted
                shouldShowRationale -> Denied
                else -> PermanentlyDenied
            }
    }
}
