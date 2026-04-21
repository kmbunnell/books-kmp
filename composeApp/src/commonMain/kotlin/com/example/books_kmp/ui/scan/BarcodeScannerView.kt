package com.example.books_kmp.ui.scan

import androidx.compose.runtime.Composable
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.domain.Result

@Composable
expect fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit)
