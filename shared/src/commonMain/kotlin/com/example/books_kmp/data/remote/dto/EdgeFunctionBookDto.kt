package com.example.books_kmp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdgeFunctionBookDto(
    val isbn: String? = null,
    val title: String,
    val authors: List<String> = emptyList(),
    @SerialName("cover_url") val coverUrl: String? = null,
)
