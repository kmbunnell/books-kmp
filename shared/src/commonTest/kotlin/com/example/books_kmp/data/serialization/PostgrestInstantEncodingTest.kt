package com.example.books_kmp.data.serialization

import com.example.books_kmp.data.library.BookDto
import com.example.books_kmp.data.library.toBook
import com.example.books_kmp.data.library.toDto
import com.example.books_kmp.data.tags.TagDto
import com.example.books_kmp.data.tags.toTag
import com.example.books_kmp.domain.model.NewBook
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * Pins [kotlinx.serialization.EncodeDefault.Mode.NEVER] on the server-maintained fields.
 * The production Postgrest client (`KotlinXSerializer` in supabase-kt) uses a plain `Json`,
 * whose `encodeDefaults` already defaults to `false` — so today this annotation is a
 * belt-and-suspenders guard, not the only thing standing between a fresh DTO and sending
 * `id`/`user_id`/`updated_at`. It keeps that guarantee explicit and makes it immune to a
 * future Json reconfiguration (e.g. `encodeDefaults = true`) elsewhere in the app.
 */
class PostgrestInstantEncodingTest {
    private val json = Json

    @Test
    fun `fresh BookDto from NewBook omits server-maintained fields on encode`() {
        val dto = NewBook(title = "Dune", authors = listOf("Frank Herbert")).toDto(userId = "u1")

        val encoded = json.encodeToString(BookDto.serializer(), dto)

        assertFalse("updated_at" in encoded, "expected no updated_at in: $encoded")
        assertFalse("\"id\"" in encoded, "expected no id in: $encoded")
        assertTrue("user_id" in encoded, "expected user_id to be sent in: $encoded")
    }

    @Test
    fun `fresh TagDto omits server-maintained fields on encode`() {
        val dto = TagDto(userId = "u1", name = "Fiction")

        val encoded = json.encodeToString(TagDto.serializer(), dto)

        assertFalse("updated_at" in encoded, "expected no updated_at in: $encoded")
        assertFalse("\"id\"" in encoded, "expected no id in: $encoded")
        assertTrue("user_id" in encoded, "expected user_id to be sent in: $encoded")
    }

    @Test
    fun `decoded BookDto re-sends updated_at if it is ever encoded again`() {
        val bookJson =
            """{"id":"b1","user_id":"u1","title":"Dune","authors":["Frank Herbert"],""" +
                """"updated_at":"2026-08-03T12:00:00Z","book_tags":[]}"""
        val dto = json.decodeFromString(BookDto.serializer(), bookJson)

        val encoded = json.encodeToString(BookDto.serializer(), dto)

        assertTrue(
            "updated_at" in encoded,
            "a decoded DTO round-tripping through encode must not silently drop updated_at: $encoded",
        )
    }

    @Test
    fun `BookDto toBook fails loudly instead of treating a missing updated_at as DISTANT_PAST`() {
        val dto = json.decodeFromString(BookDto.serializer(), """{"id":"b1","title":"Dune"}""")

        assertFailsWith<IllegalStateException> { dto.toBook() }
    }

    @Test
    fun `TagDto toTag fails loudly instead of treating a missing updated_at as DISTANT_PAST`() {
        val dto = json.decodeFromString(TagDto.serializer(), """{"id":"t1","name":"Fiction"}""")

        assertFailsWith<IllegalStateException> { dto.toTag() }
    }
}
