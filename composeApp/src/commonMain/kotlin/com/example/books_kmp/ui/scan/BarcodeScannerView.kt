package com.example.books_kmp.ui.scan

import androidx.compose.runtime.Composable
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BarcodeScanError

@Composable
expect fun BarcodeScannerView(onResult: (Result<String, BarcodeScanError>) -> Unit)
