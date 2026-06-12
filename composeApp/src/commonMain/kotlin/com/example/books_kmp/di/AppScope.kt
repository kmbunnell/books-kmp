package com.example.books_kmp.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AppScope(val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
    fun cancel() = coroutineScope.cancel()
}
