package com.example.books_kmp.data.recommendation

import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntry(
    val title: String,
    val authors: List<String>,
)
