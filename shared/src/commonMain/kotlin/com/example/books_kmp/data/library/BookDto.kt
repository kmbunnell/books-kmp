package com.example.books_kmp.data.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.NewBook
import kotlin.time.Instant
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
    // Server-maintained, like `id`/`userId`: omitted on write only while left at its default
    // (null) — a decode-then-encode round trip would resend it. See PostgrestInstantEncodingTest.
    // Null only when absent from the response; the `books` table always populates it via trigger,
    // so `toBook()` below treats a null as a data-integrity error rather than a real timestamp.
    @SerialName("updated_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val updatedAt: Instant? = null,
    @SerialName("book_tags")
    val bookTags: List<BookTagIdDto> = emptyList(),
)

@Serializable
data class BookTagIdDto(
    @SerialName("tag_id")
    val tagId: String,
)

fun BookDto.toBook(): Book =
    Book(
        id = id,
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = coverImageUrl,
        updatedAt = updatedAt ?: error("BookDto($id) is missing server-maintained updated_at"),
        tags = bookTags.map { it.tagId },
    )

fun NewBook.toDto(userId: String): BookDto =
    BookDto(
        userId = userId,
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = coverImageUrl,
    )
