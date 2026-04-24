package com.example.books_kmp.domain.model

data class Tag(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

val TAG_SORT_ORDER: Comparator<Tag> =
    compareByDescending<Tag> { it.isDefault }.thenBy { it.name.lowercase() }
