package com.example.books_kmp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenLibrarySearchDocDto(
    val title: String = "",
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("cover_i") val coverId: Int? = null,
)
