package com.example.books_kmp.data.recommendation

import com.example.books_kmp.config.GeminiConfig
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.recommendation.RecommendationError
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

class GeminiRecommendationDataSourceTest {
    private val clients = mutableListOf<HttpClient>()

    private fun buildDataSource(engine: MockEngine): GeminiRecommendationDataSource =
        GeminiRecommendationDataSource(
            httpClient = HttpClient(engine).also(clients::add),
            config = GeminiConfig(apiKey = "test-api-key"),
        )

    // Wraps an inner JSON string as the Gemini response envelope.
    // The inner JSON is placed verbatim inside a JSON string value, so it must be
    // escaped — we do that with basic replace rather than a serialization import.
    private fun geminiEnvelope(innerJson: String): String {
        val escaped = innerJson.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    @AfterTest
    fun tearDown() {
        clients.forEach { it.close() }
    }

    @Test
    fun `getRawRecommendations returns Success with mapped data on 200 with valid array`() =
        runTest {
            val innerJson =
                """[{"title":"Dune","authors":["Frank Herbert"],""" +
                    """"reason":"Epic world-building","description":"A sci-fi classic"}]"""
            val engine =
                MockEngine { _ ->
                    respond(
                        content = geminiEnvelope(innerJson),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val collection = listOf(CollectionEntry(title = "1984", authors = listOf("George Orwell")))
            val result = buildDataSource(engine).getRawRecommendations(collection)

            assertIs<Result.Success<List<RawRecommendation>>>(result)
            assertEquals(1, result.data.size)
            assertEquals("Dune", result.data[0].title)
            assertEquals(listOf("Frank Herbert"), result.data[0].authors)
            assertEquals("Epic world-building", result.data[0].reason)
            assertEquals("A sci-fi classic", result.data[0].description)
        }

    @Test
    fun `getRawRecommendations deserializes isbn when present`() =
        runTest {
            val innerJson =
                """[{"title":"Dune","authors":["Frank Herbert"],"isbn":"9780441013593",""" +
                    """"reason":"Epic world-building","description":"A sci-fi classic"}]"""
            val engine =
                MockEngine { _ ->
                    respond(
                        content = geminiEnvelope(innerJson),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val collection = listOf(CollectionEntry(title = "1984", authors = listOf("George Orwell")))
            val result = buildDataSource(engine).getRawRecommendations(collection)

            assertIs<Result.Success<List<RawRecommendation>>>(result)
            assertEquals("9780441013593", result.data[0].isbn)
        }

    @Test
    fun `getRawRecommendations returns NetworkError on HTTP 500`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":{"message":"Internal error"}}""",
                        status = HttpStatusCode.InternalServerError,
                    )
                }

            val result =
                buildDataSource(engine).getRawRecommendations(
                    listOf(CollectionEntry("1984", listOf("George Orwell"))),
                )

            assertIs<Result.Failure<RecommendationError>>(result)
            assertIs<RecommendationError.NetworkError>(result.error)
        }

    @Test
    fun `getRawRecommendations returns NoResults when Gemini returns empty array`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = geminiEnvelope("[]"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result =
                buildDataSource(engine).getRawRecommendations(
                    listOf(CollectionEntry("1984", listOf("George Orwell"))),
                )

            assertIs<Result.Failure<RecommendationError>>(result)
            assertEquals(RecommendationError.NoResults, result.error)
        }

    @Test
    fun `getRawRecommendations returns NetworkError on malformed JSON`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = geminiEnvelope("not-valid-json"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val result =
                buildDataSource(engine).getRawRecommendations(
                    listOf(CollectionEntry("1984", listOf("George Orwell"))),
                )

            assertIs<Result.Failure<RecommendationError>>(result)
            assertIs<RecommendationError.NetworkError>(result.error)
        }
}
