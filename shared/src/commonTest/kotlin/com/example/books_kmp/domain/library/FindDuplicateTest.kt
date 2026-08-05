package com.example.books_kmp.domain.library

import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.testing.TEST_INSTANT
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FindDuplicateTest {
    private fun book(
        isbn: String? = null,
        title: String = "The Iliad",
        authors: List<String> = listOf("Homer"),
    ) = Book(
        id = "1",
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = null,
        updatedAt = TEST_INSTANT,
    )

    @Test
    fun `both isbn null — matching title and overlapping author returns true`() {
        assertTrue(
            matchesDuplicate(
                book(isbn = null, title = "The Iliad", authors = listOf("Homer")),
                null,
                "the iliad",
                listOf("homer")
            )
        )
    }

    @Test
    fun `both isbn non-null and equal returns true regardless of title and author`() {
        assertTrue(
            matchesDuplicate(
                book(isbn = "111", title = "The Iliad", authors = listOf("Homer")),
                "111",
                "the odyssey",
                listOf("virgil")
            )
        )
    }

    @Test
    fun `both isbn non-null but unequal returns false even when title and author match`() {
        // Different ISBNs = different editions; title+author fallback is not reached
        assertFalse(
            matchesDuplicate(
                book(isbn = "111", title = "The Iliad", authors = listOf("Homer")),
                "222",
                "the iliad",
                listOf("homer")
            )
        )
    }

    @Test
    fun `incoming isbn non-null but stored isbn null — matching title and author returns true`() {
        assertTrue(
            matchesDuplicate(
                book(isbn = null, title = "The Iliad", authors = listOf("Homer")),
                "111",
                "the iliad",
                listOf("homer")
            )
        )
    }

    @Test
    fun `incoming isbn null but stored isbn non-null — matching title and author returns true`() {
        assertTrue(
            matchesDuplicate(
                book(isbn = "111", title = "The Iliad", authors = listOf("Homer")),
                null,
                "the iliad",
                listOf("homer")
            )
        )
    }

    @Test
    fun `both isbn null and matching title — no author overlap returns false`() {
        assertFalse(
            matchesDuplicate(
                book(isbn = null, title = "The Iliad", authors = listOf("Homer")),
                null,
                "the iliad",
                listOf("virgil")
            )
        )
    }

    @Test
    fun `both isbn null and author overlap — title mismatch returns false`() {
        assertFalse(
            matchesDuplicate(
                book(isbn = null, title = "The Iliad", authors = listOf("Homer")),
                null,
                "the odyssey",
                listOf("homer")
            )
        )
    }
}
