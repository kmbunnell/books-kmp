package com.example.books_kmp.data.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BookDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String = "",
    @SerialName("user_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val userId: String = "",
    val isbn: String? = null,
    val title: String = "",
    val authors: List<String> = emptyList(),
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,
)

fun BookDto.toBook(): Book =
    Book(
        id = id,
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = coverImageUrl,
    )

fun NewBook.toDto(userId: String): BookDto =
    BookDto(
        userId = userId,
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = coverImageUrl,
    )
