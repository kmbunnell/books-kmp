package com.example.books_kmp.domain.model

data class Book(
    val id: String,
    val isbn: String?,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val tags: List<String> = emptyList(),
)
