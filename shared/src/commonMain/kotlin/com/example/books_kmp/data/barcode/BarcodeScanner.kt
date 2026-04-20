package com.example.books_kmp.data.barcode

import com.example.books_kmp.domain.Result

expect class BarcodeScanner {
    suspend fun scanBarcode(): Result<String, BarcodeScanError>
}
