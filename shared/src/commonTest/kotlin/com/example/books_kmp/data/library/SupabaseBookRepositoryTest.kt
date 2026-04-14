package com.example.books_kmp.data.library

import com.example.books_kmp.domain.model.Book
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Integration tests for [SupabaseBookRepository].
 *
 * Requires a running local Supabase instance: `supabase start`
 *
 * Test user must exist in the local auth.users table:
 *   email: test@books.local
 *   password: Test1234!
 *
 * Create via: supabase auth create-user test@books.local --password Test1234!
 */
class SupabaseBookRepositoryTest {
    private val supabaseUrl = "http://127.0.0.1:54321"

    // Local Supabase anon key — get from `supabase status -o json | jq -r .ANON_KEY`
    private val supabaseKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9." +
            "CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"

    private val testEmail = "test@books.local"
    private val testPassword = "Test1234!"

    private val supabase =
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
        ) {
            install(Auth) { minimalSettings() }
            install(Postgrest)
        }

    private val repository = SupabaseBookRepository(supabase)

    @BeforeTest
    fun signInAndCleanup() =
        runTest {
            supabase.auth.signInWith(Email) {
                email = testEmail
                password = testPassword
            }
            // Delete all books owned by the test user for a clean slate.
            // A filter is required by postgrest-kt; RLS scopes the delete to this user's rows only.
            supabase.from("books").delete {
                filter {
                    neq("id", "00000000-0000-0000-0000-000000000000")
                }
            }
        }

    @AfterTest
    fun signOut() =
        runTest {
            supabase.auth.signOut()
        }

    @Test
    fun `addBook returns book with server-generated id`() =
        runTest {
            val book = testBook(isbn = "9780140449136")
            val result = repository.addBook(book)
            assertTrue(result.id.isNotBlank(), "Expected a server-generated non-blank id")
        }

    @Test
    fun `addBook persists correct title authors isbn and cover_image_url`() =
        runTest {
            val book =
                testBook(
                    isbn = "9780140449136",
                    title = "The Iliad",
                    authors = listOf("Homer"),
                    coverImageUrl = "https://covers.openlibrary.org/b/isbn/9780140449136-L.jpg",
                )
            val result = repository.addBook(book)
            assertEquals("The Iliad", result.title)
            assertEquals(listOf("Homer"), result.authors)
            assertEquals("9780140449136", result.isbn)
            assertEquals(
                "https://covers.openlibrary.org/b/isbn/9780140449136-L.jpg",
                result.coverImageUrl,
            )
        }

    @Test
    fun `addBook succeeds when isbn is null`() =
        runTest {
            val book = testBook(isbn = null, title = "No ISBN Book")
            val result = repository.addBook(book)
            assertNull(result.isbn)
            assertEquals("No ISBN Book", result.title)
        }

    @Test
    fun `addBook succeeds when cover_image_url is null`() =
        runTest {
            val book = testBook(isbn = "9780140449136", coverImageUrl = null)
            val result = repository.addBook(book)
            assertNull(result.coverImageUrl)
        }

    @Test
    fun `getBooksByUser returns only the authenticated user's books`() =
        runTest {
            repository.addBook(testBook(isbn = "1111111111111", title = "Book One"))
            repository.addBook(testBook(isbn = "2222222222222", title = "Book Two"))
            val books = repository.getBooksByUser()
            assertEquals(2, books.size)
            assertTrue(books.any { it.title == "Book One" })
            assertTrue(books.any { it.title == "Book Two" })
        }

    @Test
    fun `getBookByIsbn returns matching book when isbn exists`() =
        runTest {
            repository.addBook(testBook(isbn = "9780743273565", title = "The Great Gatsby"))
            val result = repository.getBookByIsbn("9780743273565")
            assertNotNull(result)
            assertEquals("The Great Gatsby", result.title)
        }

    @Test
    fun `getBookByIsbn returns null when isbn is not found`() =
        runTest {
            val result = repository.getBookByIsbn("0000000000000")
            assertNull(result)
        }

    @Test
    fun `isbnExists returns true when isbn matches a persisted book`() =
        runTest {
            repository.addBook(testBook(isbn = "9780140449136", title = "The Iliad"))
            assertTrue(repository.isbnExists("9780140449136"))
        }

    @Test
    fun `addBook sets user_id from authenticated session via RLS`() =
        runTest {
            val authUser = checkNotNull(supabase.auth.currentUserOrNull()) { "Must be authenticated before this test runs" }
            repository.addBook(testBook(isbn = "9780140449136"))
            val rows = supabase.from("books").select("user_id").decodeList<UserIdRow>()
            assertEquals(1, rows.size)
            assertEquals(authUser.id, rows.first().userId)
        }

    @Serializable
    private data class UserIdRow(
        @SerialName("user_id") val userId: String,
    )

    private fun testBook(
        isbn: String? = null,
        title: String = "Test Book",
        authors: List<String> = listOf("Test Author"),
        coverImageUrl: String? = null,
    ): Book =
        Book(
            id = "",
            isbn = isbn,
            title = title,
            authors = authors,
            coverImageUrl = coverImageUrl,
        )
}
