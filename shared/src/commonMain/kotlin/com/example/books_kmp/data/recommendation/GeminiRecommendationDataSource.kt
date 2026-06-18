package com.example.books_kmp.data.recommendation

import com.example.books_kmp.config.GeminiConfig
import com.example.books_kmp.domain.MAX_RECOMMENDED_BOOKS
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.recommendation.RecommendationError
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val RECOMMENDATION_PROMPT_TEMPLATE =
    "You are a book recommendation engine. Analyze this reading list and infer the reader's " +
        "preferred genres, themes, and writing styles:\n" +
        "{BOOK_LIST}\n\n" +
        "Recommend exactly {COUNT} books that align with the reader's demonstrated tastes. Requirements:\n" +
        "- Do not recommend books already in the list\n" +
        "- If a book is part of a series, only recommend the first book in that series\n" +
        "- Vary the {COUNT} recommendations across the inferred genres and themes where possible\n" +
        "- In the \"reason\" field, reference at least one specific book from the reading list " +
        "to explain why this recommendation fits\n\n" +
        "Return a JSON array with objects:\n" +
        "{\"title\":\"...\",\"authors\":[...],\"reason\":\"...\",\"description\":\"...\"}"

private const val GEMINI_BASE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
private data class GeminiCandidate(val content: GeminiContent)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
private data class GeminiRequestContent(val parts: List<GeminiPart>)

@Serializable
private data class GeminiGenerationConfig(
    @SerialName("response_mime_type") val responseMimeType: String,
)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiRequestContent>,
    val generationConfig: GeminiGenerationConfig,
)

class GeminiRecommendationDataSource(
    private val httpClient: HttpClient,
    private val config: GeminiConfig,
) : RecommendationDataSource {
    override suspend fun getRawRecommendations(
        collection: List<CollectionEntry>,
    ): Result<List<RawRecommendation>, RecommendationError> {
        val prompt = buildPrompt(collection)
        val requestBody =
            GeminiRequest(
                contents = listOf(GeminiRequestContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json"),
            )

        return try {
            val response =
                httpClient.post(GEMINI_BASE_URL) {
                    header("x-goog-api-key", config.apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(GeminiRequest.serializer(), requestBody))
                }

            if (response.status != HttpStatusCode.OK) {
                return Result.Failure(
                    RecommendationError.NetworkError(
                        RuntimeException("HTTP ${response.status.value}")
                    )
                )
            }

            val geminiResponse: GeminiResponse = json.decodeFromString(response.bodyAsText())
            val text =
                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return Result.Failure(RecommendationError.NoResults)

            val recommendations: List<RawRecommendation> = json.decodeFromString(text)
            if (recommendations.isEmpty()) {
                Result.Failure(RecommendationError.NoResults)
            } else {
                Result.Success(recommendations)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(RecommendationError.NetworkError(e))
        }
    }

    private fun buildPrompt(collection: List<CollectionEntry>): String {
        val bookList =
            collection.joinToString("\n") { entry ->
                "${entry.title} by ${entry.authors.joinToString(", ")}"
            }
        return RECOMMENDATION_PROMPT_TEMPLATE
            .replace("{BOOK_LIST}", bookList)
            .replace("{COUNT}", MAX_RECOMMENDED_BOOKS.toString())
    }
}
