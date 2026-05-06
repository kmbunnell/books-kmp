package com.example.books_kmp.domain.model

data class BookLookupData(
    val isbn: String?,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
)
