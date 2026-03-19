package com.example.books_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
