package com.example.books_kmp.data.local.database

import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * For a file-backed URL, [app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver] opens a new
 * JDBC connection per thread rather than reusing one. A one-shot `PRAGMA foreign_keys=ON`
 * executed on the constructing thread therefore does not apply to connections opened by other
 * threads, silently disabling cascades everywhere except the thread that built the driver.
 * [BooksDatabaseSchemaTest] can't catch this: it uses `JdbcSqliteDriver.IN_MEMORY`, which is the
 * one URL form that shares a single connection.
 *
 * This test calls the actual desktop driver factory ([createDesktopDriver]) against a real
 * file-backed database, performing every operation from a thread other than the one that built
 * the driver -- so a regression in the production connection-properties fix fails this test
 * instead of only being caught by a re-implementation of it.
 */
class DesktopDriverForeignKeyTest {
    @Test
    fun `book delete cascades to book_tags from a different thread than the one that built the driver`() {
        val tempDir = Files.createTempDirectory("books-fk-test")
        val dbFile = tempDir.resolve("books-fk-test.db").toFile()

        val driver = createDesktopDriver("jdbc:sqlite:${dbFile.absolutePath}")
        val database = BooksDatabase(driver)
        val executor = Executors.newSingleThreadExecutor()

        try {
            executor
                .submit {
                    database.booksQueries.insert(
                        id = "book-1",
                        user_id = "user-1",
                        isbn = "9780306406157",
                        title = "Test Book",
                        authors = "[]",
                        cover_image_url = null,
                        created_at = 0L,
                        updated_at = 0L,
                    )
                    database.tagsQueries.insert(
                        id = "tag-1",
                        user_id = "user-1",
                        name = "Sci-Fi",
                        is_default = 0L,
                        created_at = 0L,
                        updated_at = 0L,
                    )
                    database.bookTagsQueries.insert(book_id = "book-1", tag_id = "tag-1")
                    driver.execute(null, "DELETE FROM books WHERE id = 'book-1';", 0)
                }.get()

            val remaining =
                executor.submit<List<Book_tags>> { database.bookTagsQueries.selectAll().executeAsList() }.get()

            assertEquals(emptyList(), remaining)
        } finally {
            executor.shutdown()
            driver.close()
            tempDir.toFile().deleteRecursively()
        }
    }
}
