package com.example.books_kmp.data.serialization

import com.example.books_kmp.data.library.BookDto
import com.example.books_kmp.data.tags.TagDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class PostgrestInstantDecodingTest {
    private val json = Json

    @Test
    fun `BookDto decodes updated_at with microseconds and colon offset`() {
        val bookJson =
            """{"id":"b1","user_id":"u1","title":"Dune","authors":["Frank Herbert"],""" +
                """"updated_at":"2026-08-03T12:34:56.789012+00:00","book_tags":[]}"""

        val dto = json.decodeFromString(BookDto.serializer(), bookJson)

        assertEquals(Instant.parse("2026-08-03T12:34:56.789012Z"), dto.updatedAt)
    }

    @Test
    fun `BookDto updated_at is null when absent from the response`() {
        val dto = json.decodeFromString(BookDto.serializer(), "{}")

        assertEquals(null, dto.updatedAt)
    }

    @Test
    fun `TagDto decodes updated_at with no fractional seconds`() {
        val tagJson =
            """{"id":"t1","user_id":"u1","name":"Fiction","is_default":false,""" +
                """"updated_at":"2026-08-03T12:34:56+00:00"}"""

        val dto = json.decodeFromString(TagDto.serializer(), tagJson)

        assertEquals(Instant.parse("2026-08-03T12:34:56Z"), dto.updatedAt)
    }

    @Test
    fun `TagDto decodes updated_at with short two-digit UTC offset`() {
        val tagJson =
            """{"id":"t1","user_id":"u1","name":"Fiction","is_default":false,""" +
                """"updated_at":"2026-08-03T12:34:56+00"}"""

        val dto = json.decodeFromString(TagDto.serializer(), tagJson)

        assertEquals(Instant.parse("2026-08-03T12:34:56Z"), dto.updatedAt)
    }

    @Test
    fun `TagDto normalises a non-UTC session offset to UTC`() {
        val tagJson =
            """{"id":"t1","user_id":"u1","name":"Fiction","is_default":false,""" +
                """"updated_at":"2026-08-03T08:34:56.789-04:00"}"""

        val dto = json.decodeFromString(TagDto.serializer(), tagJson)

        assertEquals(Instant.parse("2026-08-03T12:34:56.789Z"), dto.updatedAt)
    }

    @Test
    fun `TagDto updated_at is null when absent from the response`() {
        val dto = json.decodeFromString(TagDto.serializer(), "{}")

        assertEquals(null, dto.updatedAt)
    }
}
