package com.example.books_kmp.domain.model

data class NewBook(
    val title: String,
    val authors: List<String>,
    val isbn: String? = null,
    val coverImageUrl: String? = null,
)
