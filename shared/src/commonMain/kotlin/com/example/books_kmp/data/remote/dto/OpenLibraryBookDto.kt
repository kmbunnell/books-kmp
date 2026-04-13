package com.example.books_kmp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenLibraryBookDto(
    val title: String? = null,
    val authors: List<AuthorDto>? = null,
    val cover: CoverDto? = null,
)

@Serializable
data class AuthorDto(val name: String? = null)

@Serializable
data class CoverDto(val medium: String? = null)
