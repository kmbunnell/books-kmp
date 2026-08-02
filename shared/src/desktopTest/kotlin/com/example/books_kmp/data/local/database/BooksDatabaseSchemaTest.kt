package com.example.books_kmp.data.local.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Schema smoke test: proves the generated [BooksDatabase] creates every table, that rows
 * round-trip through insert/select, that the `book_tags` foreign keys cascade on delete, and
 * that `created_at`/`updated_at` preserve full epoch-millisecond precision.
 */
class BooksDatabaseSchemaTest {
    /** Epoch millis with a deliberately non-zero millisecond component (`...123`). */
    private val createdAt = 1_753_970_400_123L
    private val updatedAt = 1_753_970_460_456L

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: BooksDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BooksDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0)
        database = BooksDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `books row round-trips through insert and select`() {
        database.booksQueries.insert(
            id = "book-1",
            user_id = "user-1",
            isbn = "9780306406157",
            title = "Test Book",
            authors = """["Ada Lovelace","Alan Turing"]""",
            cover_image_url = "https://example.com/cover.jpg",
            created_at = createdAt,
            updated_at = updatedAt,
        )

        val books = database.booksQueries.selectAll().executeAsList()

        assertEquals(1, books.size)
        val book = books.single()
        assertEquals("book-1", book.id)
        assertEquals("user-1", book.user_id)
        assertEquals("9780306406157", book.isbn)
        assertEquals("Test Book", book.title)
        assertEquals("""["Ada Lovelace","Alan Turing"]""", book.authors)
        assertEquals("https://example.com/cover.jpg", book.cover_image_url)
    }

    @Test
    fun `books allows null isbn and null cover image url`() {
        insertBook(id = "book-1", isbn = null)
        insertBook(id = "book-2", isbn = null)

        val books = database.booksQueries.selectAll().executeAsList()

        assertEquals(2, books.size)
        assertTrue(books.all { it.isbn == null })
        assertTrue(books.all { it.cover_image_url == null })
    }

    @Test
    fun `books rejects a duplicate non-null isbn for the same user`() {
        insertBook(id = "book-1", isbn = "9780306406157")

        val failure =
            runCatching {
                insertBook(id = "book-2", isbn = "9780306406157")
            }

        assertTrue(
            failure.exceptionOrNull()?.message?.contains("UNIQUE") == true,
            "expected UNIQUE(user_id, isbn) to reject the duplicate ISBN, got $failure",
        )
    }

    @Test
    fun `books allows the same isbn for two different users`() {
        insertBook(id = "book-1", userId = "user-1", isbn = "9780306406157")
        insertBook(id = "book-2", userId = "user-2", isbn = "9780306406157")

        val books = database.booksQueries.selectAll().executeAsList()

        assertEquals(2, books.size)
    }

    @Test
    fun `tags row round-trips through insert and select`() {
        database.tagsQueries.insert(
            id = "tag-1",
            user_id = "user-1",
            name = "Sci-Fi",
            is_default = 1L,
            created_at = createdAt,
            updated_at = updatedAt,
        )

        val tag = database.tagsQueries.selectAll().executeAsList().single()

        assertEquals("tag-1", tag.id)
        assertEquals("user-1", tag.user_id)
        assertEquals("Sci-Fi", tag.name)
        assertEquals(1L, tag.is_default)
    }

    @Test
    fun `tags rejects a duplicate name for the same user`() {
        insertTag(id = "tag-1", name = "Sci-Fi")

        val failure = runCatching { insertTag(id = "tag-2", name = "Sci-Fi") }

        assertTrue(
            failure.exceptionOrNull()?.message?.contains("UNIQUE") == true,
            "expected UNIQUE(user_id, name) to reject the duplicate tag name, got $failure",
        )
    }

    @Test
    fun `tags allows the same name for two different users`() {
        insertTag(id = "tag-1", userId = "user-1", name = "Sci-Fi")
        insertTag(id = "tag-2", userId = "user-2", name = "Sci-Fi")

        val tags = database.tagsQueries.selectAll().executeAsList()

        assertEquals(2, tags.size)
    }

    @Test
    fun `book_tags row round-trips through insert and select`() {
        insertBook(id = "book-1", isbn = "9780306406157")
        insertTag(id = "tag-1", name = "Sci-Fi")

        database.bookTagsQueries.insert(book_id = "book-1", tag_id = "tag-1")

        val join = database.bookTagsQueries.selectAll().executeAsList().single()
        assertEquals("book-1", join.book_id)
        assertEquals("tag-1", join.tag_id)
    }

    @Test
    fun `book_tags rejects a row referencing a missing book`() {
        insertTag(id = "tag-1", name = "Sci-Fi")

        val failure =
            runCatching {
                database.bookTagsQueries.insert(book_id = "missing-book", tag_id = "tag-1")
            }

        assertTrue(
            failure.exceptionOrNull()?.message?.contains("FOREIGN KEY") == true,
            "expected the book_id foreign key to reject an orphan row, got $failure",
        )
    }

    @Test
    fun `deleting a book cascades to its book_tags rows`() {
        insertBook(id = "book-1", isbn = "9780306406157")
        insertBook(id = "book-2", isbn = "9780262033848")
        insertTag(id = "tag-1", name = "Sci-Fi")
        database.bookTagsQueries.insert(book_id = "book-1", tag_id = "tag-1")
        database.bookTagsQueries.insert(book_id = "book-2", tag_id = "tag-1")

        driver.execute(null, "DELETE FROM books WHERE id = 'book-1';", 0)

        val remaining = database.bookTagsQueries.selectAll().executeAsList()
        assertEquals(1, remaining.size)
        assertEquals("book-2", remaining.single().book_id)
    }

    @Test
    fun `deleting a tag cascades to its book_tags rows`() {
        insertBook(id = "book-1", isbn = "9780306406157")
        insertTag(id = "tag-1", name = "Sci-Fi")
        database.bookTagsQueries.insert(book_id = "book-1", tag_id = "tag-1")

        driver.execute(null, "DELETE FROM tags WHERE id = 'tag-1';", 0)

        assertEquals(emptyList(), database.bookTagsQueries.selectAll().executeAsList())
    }

    @Test
    fun `pending_operations row round-trips through insert and select`() {
        database.pendingOperationsQueries.insert(
            id = "op-1",
            operation_type = "UPSERT_BOOK",
            payload = """{"id":"book-1"}""",
            created_at = createdAt,
            retry_count = 0L,
        )

        val operation = database.pendingOperationsQueries.selectAll().executeAsList().single()

        assertEquals("op-1", operation.id)
        assertEquals("UPSERT_BOOK", operation.operation_type)
        assertEquals("""{"id":"book-1"}""", operation.payload)
        assertEquals(createdAt, operation.created_at)
        assertEquals(0L, operation.retry_count)
    }

    @Test
    fun `timestamps round-trip with full millisecond precision`() {
        insertBook(id = "book-1", isbn = "9780306406157")
        insertTag(id = "tag-1", name = "Sci-Fi")

        val book = database.booksQueries.selectAll().executeAsList().single()
        assertEquals(createdAt, book.created_at)
        assertEquals(updatedAt, book.updated_at)
        assertEquals(123L, book.created_at % 1000L, "sub-second precision was truncated")
        assertEquals(456L, book.updated_at % 1000L, "sub-second precision was truncated")

        val tag = database.tagsQueries.selectAll().executeAsList().single()
        assertEquals(createdAt, tag.created_at)
        assertEquals(updatedAt, tag.updated_at)
    }

    @Test
    fun `all four tables start empty`() {
        assertEquals(emptyList(), database.booksQueries.selectAll().executeAsList())
        assertEquals(emptyList(), database.tagsQueries.selectAll().executeAsList())
        assertEquals(emptyList(), database.bookTagsQueries.selectAll().executeAsList())
        assertEquals(emptyList(), database.pendingOperationsQueries.selectAll().executeAsList())
        assertNull(database.booksQueries.selectAll().executeAsOneOrNull())
    }

    private fun insertBook(
        id: String,
        isbn: String?,
        userId: String = "user-1",
    ) = database.booksQueries.insert(
        id = id,
        user_id = userId,
        isbn = isbn,
        title = "Test Book",
        authors = "[]",
        cover_image_url = null,
        created_at = createdAt,
        updated_at = updatedAt,
    )

    private fun insertTag(
        id: String,
        name: String,
        userId: String = "user-1",
    ) = database.tagsQueries.insert(
        id = id,
        user_id = userId,
        name = name,
        is_default = 0L,
        created_at = createdAt,
        updated_at = updatedAt,
    )
}
