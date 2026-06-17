package com.example.books_kmp.data.recommendation

import kotlinx.serialization.Serializable

@Serializable
data class RawRecommendation(
    val title: String,
    val authors: List<String>,
    val reason: String,
    val description: String? = null,
)
