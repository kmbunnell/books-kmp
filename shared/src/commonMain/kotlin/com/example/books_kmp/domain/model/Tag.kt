package com.example.books_kmp.domain.model

import kotlin.time.Instant

data class Tag(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val updatedAt: Instant,
)

val TAG_SORT_ORDER: Comparator<Tag> =
    compareByDescending<Tag> { it.isDefault }.thenBy { it.name.lowercase() }
