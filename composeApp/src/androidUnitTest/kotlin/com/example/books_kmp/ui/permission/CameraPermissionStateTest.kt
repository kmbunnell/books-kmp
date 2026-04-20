package com.example.books_kmp.ui.permission

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraPermissionStateTest {
    @Test
    fun `granted result transitions to Granted`() {
        val result = CameraPermissionState.fromResult(granted = true, shouldShowRationale = false)
        assertEquals(CameraPermissionState.Granted, result)
    }

    @Test
    fun `denied result with shouldShowRationale true transitions to Denied`() {
        val result = CameraPermissionState.fromResult(granted = false, shouldShowRationale = true)
        assertEquals(CameraPermissionState.Denied, result)
    }

    @Test
    fun `denied result with shouldShowRationale false transitions to PermanentlyDenied`() {
        val result = CameraPermissionState.fromResult(granted = false, shouldShowRationale = false)
        assertEquals(CameraPermissionState.PermanentlyDenied, result)
    }
}
