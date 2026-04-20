package com.example.books_kmp.data.barcode

import com.example.books_kmp.domain.Result

actual class BarcodeScanner {
    actual suspend fun scanBarcode(): Result<String, BarcodeScanError> = Result.Failure(BarcodeScanError.Cancelled)
}
