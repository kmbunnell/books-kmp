package com.example.books_kmp.data.tags

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookTagDto(
    @SerialName("book_id")
    val bookId: String,
    @SerialName("tag_id")
    val tagId: String,
)
