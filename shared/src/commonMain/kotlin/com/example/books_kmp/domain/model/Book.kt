package com.example.books_kmp.domain.model

import kotlin.time.Instant

data class Book(
    val id: String,
    val isbn: String?,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val updatedAt: Instant,
    val tags: List<String> = emptyList(),
)
