package com.example.books_kmp

interface Platform {
    val name: String
    val isCameraScanSupported: Boolean
}

expect fun getPlatform(): Platform
