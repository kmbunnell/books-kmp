package com.example.books_kmp.data.remote.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class OpenLibrarySearchDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes full search response with multiple docs`() {
        val raw =
            """
            {
              "docs": [
                {
                  "title": "Dune",
                  "author_name": ["Frank Herbert"],
                  "cover_i": 7965445
                },
                {
                  "title": "Foundation",
                  "author_name": ["Isaac Asimov"],
                  "cover_i": 1234567
                }
              ]
            }
            """.trimIndent()

        val response = json.decodeFromString<OpenLibrarySearchResponseDto>(raw)

        assertEquals(2, response.docs.size)
        assertEquals("Dune", response.docs[0].title)
        assertEquals(listOf("Frank Herbert"), response.docs[0].authorName)
        assertEquals(7965445, response.docs[0].coverId)
        assertEquals("Foundation", response.docs[1].title)
        assertEquals(listOf("Isaac Asimov"), response.docs[1].authorName)
        assertEquals(1234567, response.docs[1].coverId)
    }

    @Test
    fun `decodes search response with empty docs array`() {
        val raw = """{"docs": []}"""

        val response = json.decodeFromString<OpenLibrarySearchResponseDto>(raw)

        assertEquals(emptyList(), response.docs)
    }

    @Test
    fun `decodes search doc with null coverId`() {
        val raw =
            """
            {
              "docs": [
                {
                  "title": "No Cover Book",
                  "author_name": ["Some Author"]
                }
              ]
            }
            """.trimIndent()

        val response = json.decodeFromString<OpenLibrarySearchResponseDto>(raw)

        assertNull(response.docs[0].coverId)
    }

    @Test
    fun `decodes search doc with multiple authors`() {
        val raw =
            """
            {
              "docs": [
                {
                  "title": "Co-authored Book",
                  "author_name": ["Author One", "Author Two", "Author Three"]
                }
              ]
            }
            """.trimIndent()

        val response = json.decodeFromString<OpenLibrarySearchResponseDto>(raw)

        assertEquals(listOf("Author One", "Author Two", "Author Three"), response.docs[0].authorName)
    }

    @Test
    fun `OpenLibraryBookDto still deserializes without coverId field`() {
        val raw =
            """
            {
              "title": "Nineteen Eighty-Four",
              "authors": [{"name": "George Orwell"}],
              "cover": {"medium": "https://covers.openlibrary.org/b/id/8575708-M.jpg"}
            }
            """.trimIndent()

        val dto = json.decodeFromString<OpenLibraryBookDto>(raw)

        assertEquals("Nineteen Eighty-Four", dto.title)
        assertNull(dto.coverId)
    }

    @Test
    fun `OpenLibraryBookDto deserializes coverId when present`() {
        val raw =
            """
            {
              "title": "Dune",
              "authors": [{"name": "Frank Herbert"}],
              "cover": {"medium": "https://covers.openlibrary.org/b/id/7965445-M.jpg"},
              "cover_i": 12345
            }
            """.trimIndent()

        val dto = json.decodeFromString<OpenLibraryBookDto>(raw)

        assertEquals(12345, dto.coverId)
    }
}
