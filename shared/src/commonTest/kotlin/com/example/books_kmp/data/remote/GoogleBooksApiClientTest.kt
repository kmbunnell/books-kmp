package com.example.books_kmp.data.remote

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GoogleBooksApiClientTest {
    private val clients = mutableListOf<HttpClient>()
    private val supabaseUrl = "https://project.supabase.co"

    private fun buildClient(
        engine: MockEngine,
        token: String? = "test-jwt",
    ): GoogleBooksApiClient =
        GoogleBooksApiClient(
            httpClient = HttpClient(engine).also(clients::add),
            accessTokenProvider = { token },
            supabaseUrl = supabaseUrl,
        )

    private fun HttpRequestData.bodyText(): String {
        val content = body
        return when (content) {
            is TextContent -> content.text
            else -> content.toString()
        }
    }

    @AfterTest
    fun tearDown() {
        clients.forEach { it.close() }
    }

    // --- no-session ---

    @Test
    fun `lookupByIsbn returns Unauthenticated and makes no network call when session is missing`() =
        runTest {
            var callCount = 0
            val engine =
                MockEngine { _ ->
                    callCount++
                    respond(content = "{}", status = HttpStatusCode.OK)
                }

            val result = buildClient(engine, token = null).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.Unauthenticated, result.error)
            assertEquals(0, callCount)
        }

    @Test
    fun `lookupByTitle returns Unauthenticated and makes no network call when session is missing`() =
        runTest {
            var callCount = 0
            val engine =
                MockEngine { _ ->
                    callCount++
                    respond(content = "[]", status = HttpStatusCode.OK)
                }

            val result = buildClient(engine, token = null).lookupByTitle("Iliad")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.Unauthenticated, result.error)
            assertEquals(0, callCount)
        }

    // --- lookupByIsbn ---

    @Test
    fun `lookupByIsbn returns Success with mapped data on 200`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content =
                            """
                            {
                              "isbn": "9780451524935",
                              "title": "Nineteen Eighty-Four",
                              "authors": ["George Orwell"],
                              "cover_url": "https://example.com/cover.jpg"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Success<BookLookupData>>(result)
            assertEquals("9780451524935", result.data.isbn)
            assertEquals("Nineteen Eighty-Four", result.data.title)
            assertEquals(listOf("George Orwell"), result.data.authors)
            assertEquals("https://example.com/cover.jpg", result.data.coverImageUrl)
        }

    @Test
    fun `lookupByIsbn returns NotFound on 404`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Book not found"}""",
                        status = HttpStatusCode.NotFound,
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780000000000")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.NotFound, result.error)
        }

    @Test
    fun `lookupByIsbn returns Unauthenticated on 401`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.Unauthenticated, result.error)
        }

    @Test
    fun `lookupByIsbn returns RateLimited on 429`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Rate limit exceeded"}""",
                        status = HttpStatusCode(429, "Too Many Requests"),
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.RateLimited, result.error)
        }

    @Test
    fun `lookupByIsbn returns NetworkError on 502`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Upstream book provider failed"}""",
                        status = HttpStatusCode.BadGateway,
                    )
                }

            val result = buildClient(engine).lookupByIsbn("9780451524935")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.NetworkError, result.error)
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
            assertEquals(BookLookupError.NetworkError, result.error)
        }

    @Test
    fun `lookupByIsbn sends Authorization Bearer header with access token`() =
        runTest {
            var capturedAuth: String? = null
            val engine =
                MockEngine { request ->
                    capturedAuth = request.headers[HttpHeaders.Authorization]
                    respond(
                        content = """{"isbn":"x","title":"t","authors":[],"cover_url":null}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            buildClient(engine, token = "abc123").lookupByIsbn("9780451524935")

            assertEquals("Bearer abc123", capturedAuth)
        }

    @Test
    fun `lookupByIsbn posts JSON body with type isbn and isbn value`() =
        runTest {
            var capturedBody: String? = null
            var capturedUrl: String? = null
            val engine =
                MockEngine { request ->
                    capturedBody = request.bodyText()
                    capturedUrl = request.url.toString()
                    respond(
                        content = """{"isbn":"x","title":"t","authors":[],"cover_url":null}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            buildClient(engine).lookupByIsbn("9780451524935")

            val body = capturedBody!!
            assertTrue(body.contains("\"type\":\"isbn\""), "body was: $body")
            assertTrue(body.contains("\"isbn\":\"9780451524935\""), "body was: $body")
            assertEquals("$supabaseUrl/functions/v1/lookup-book", capturedUrl)
        }

    // --- lookupByTitle ---

    @Test
    fun `lookupByTitle returns Success with mapped list on 200 array`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content =
                            """
                            [
                              {"isbn":null,"title":"Iliad","authors":["Homer"],"cover_url":"https://x/c.jpg"},
                              {"isbn":null,"title":"Odyssey","authors":["Homer"],"cover_url":null}
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByTitle("Homer")

            assertIs<Result.Success<List<BookLookupData>>>(result)
            assertEquals(2, result.data.size)
            assertEquals("Iliad", result.data[0].title)
            assertEquals(listOf("Homer"), result.data[0].authors)
            assertEquals("https://x/c.jpg", result.data[0].coverImageUrl)
            assertEquals("Odyssey", result.data[1].title)
            assertEquals(null, result.data[1].coverImageUrl)
        }

    @Test
    fun `lookupByTitle returns Success with empty list on 200 empty array`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result = buildClient(engine).lookupByTitle("unknown")

            assertIs<Result.Success<List<BookLookupData>>>(result)
            assertTrue(result.data.isEmpty())
        }

    @Test
    fun `lookupByTitle returns Unauthenticated on 401`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                    )
                }

            val result = buildClient(engine).lookupByTitle("Iliad")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.Unauthenticated, result.error)
        }

    @Test
    fun `lookupByTitle returns NetworkError on 502`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"Upstream book provider failed"}""",
                        status = HttpStatusCode.BadGateway,
                    )
                }

            val result = buildClient(engine).lookupByTitle("Iliad")

            assertIs<Result.Failure<BookLookupError>>(result)
            assertEquals(BookLookupError.NetworkError, result.error)
        }

    @Test
    fun `lookupByTitle posts JSON body with type title and query value`() =
        runTest {
            var capturedBody: String? = null
            var capturedUrl: String? = null
            val engine =
                MockEngine { request ->
                    capturedBody = request.bodyText()
                    capturedUrl = request.url.toString()
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            buildClient(engine).lookupByTitle("The Great Gatsby")

            val body = capturedBody!!
            assertTrue(body.contains("\"type\":\"title\""), "body was: $body")
            assertTrue(body.contains("\"query\":\"The Great Gatsby\""), "body was: $body")
            assertEquals("$supabaseUrl/functions/v1/lookup-book", capturedUrl)
        }
}
