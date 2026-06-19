package com.example.books_kmp.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

internal fun ViewModel.launchIfIdle(isLoading: () -> Boolean, block: suspend () -> Unit) {
    if (isLoading()) return
    viewModelScope.launch { block() }
}
