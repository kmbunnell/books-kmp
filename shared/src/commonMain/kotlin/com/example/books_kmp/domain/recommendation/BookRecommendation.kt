package com.example.books_kmp.domain.recommendation

data class BookRecommendation(
    val title: String,
    val authors: List<String>,
    val isbn: String?,
    val coverUrl: String?,
    val reason: String,
    val description: String,
)
