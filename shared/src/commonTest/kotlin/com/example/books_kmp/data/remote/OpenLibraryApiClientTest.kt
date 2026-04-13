package com.example.books_kmp.data.remote

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class OpenLibraryApiClientTest {
    private val clients = mutableListOf<HttpClient>()

    private fun buildClient(engine: MockEngine): OpenLibraryApiClient =
        HttpClient(engine).also(clients::add).let(::OpenLibraryApiClient)

    @AfterTest
    fun tearDown() {
        clients.forEach { it.close() }
    }

    @Test
    fun `lookupByIsbn returns Success with correct title authors and coverImageUrl for valid ISBN response`() =
        runTest {
            val isbn = "9780451524935"
            val engine =
                MockEngine { _ ->
                    respond(
                        content =
                            """
                            {
                              "ISBN:9780451524935": {
                                "title": "Nineteen Eighty-Four",
                                "authors": [{"name": "George Orwell"}],
                                "cover": {"medium": "https://covers.openlibrary.org/b/id/8575708-M.jpg"}
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn(isbn)

            assertIs<Result.Success<BookLookupData>>(result)
            assertEquals("Nineteen Eighty-Four", result.data.title)
            assertEquals(listOf("George Orwell"), result.data.authors)
            assertEquals(
                "https://covers.openlibrary.org/b/id/8575708-M.jpg",
                result.data.coverImageUrl,
            )
        }

    @Test
    fun `lookupByIsbn returns NotFound when response is empty JSON object`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780000000000")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.NotFound, result.error)
        }

    @Test
    fun `lookupByIsbn returns RateLimited on HTTP 429`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode(429, "Too Many Requests"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.RateLimited, result.error)
        }

    @Test
    fun `lookupByIsbn returns MalformedResponse when JSON cannot be parsed`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "not valid json {{{",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.MalformedResponse, result.error)
        }

    @Test
    fun `lookupByIsbn returns MalformedResponse when book entry has no title`() =
        runTest {
            val isbn = "9780451524935"
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"ISBN:$isbn": {"authors": [{"name": "George Orwell"}]}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn(isbn)

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.MalformedResponse, result.error)
        }

    @Test
    fun `lookupByIsbn returns NetworkError on IOException`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    throw RuntimeException("Connection refused")
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertIs<BookLookupError.NetworkError>(result.error)
        }

    @Test
    fun `User-Agent header Shelved 1 0 is present on outbound request`() =
        runTest {
            var capturedUserAgent: String? = null
            val engine =
                MockEngine { request ->
                    capturedUserAgent = request.headers[HttpHeaders.UserAgent]
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            buildClient(engine).lookupByIsbn("9780451524935")

            assertEquals("Shelved/1.0", capturedUserAgent)
        }
}
