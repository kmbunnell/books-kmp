package com.example.books_kmp.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BarcodeScanError

@Composable
actual fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit) {
    // Desktop has no camera scanning in this ticket's scope.
    LaunchedEffect(Unit) {
        onResult(Result.Failure(BarcodeScanError.HardwareUnavailable))
    }
}
