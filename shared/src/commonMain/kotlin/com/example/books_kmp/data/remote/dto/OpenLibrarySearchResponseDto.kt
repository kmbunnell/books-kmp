package com.example.books_kmp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenLibrarySearchResponseDto(
    @SerialName("docs") val docs: List<OpenLibrarySearchDocDto> = emptyList(),
)
