
package com.example.books_kmp.data.recommendation

import com.example.books_kmp.config.GroqConfig
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
        "Recommend exactly {COUNT} books that align with the /. Requirements:\n" +
        "- Do not recommend books already in the list\n" +
        "- If a book is part of a series, only recommend the first book in that series\n" +
        "- Vary the {COUNT} recommendations across the inferred genres and themes where possible\n" +
        "- In the \"reason\" field, reference at least one specific book from the reading list " +
        "to explain why this recommendation fits\n\n" +
        "Return a JSON object with a single key \"recommendations\" containing an array of objects:\n" +
        "{\"recommendations\":[{\"title\":\"...\",\"authors\":[...],\"reason\":\"...\",\"description\":\"...\"}]}"

private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val GROQ_MODEL = "llama-3.3-70b-versatile"

@Serializable
private data class GroqMessage(val role: String, val content: String)

@Serializable
private data class GroqResponseFormat(
    @SerialName("type") val type: String
)

@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("response_format") val responseFormat: GroqResponseFormat,
)

@Serializable
private data class GroqChoice(val message: GroqMessage)

@Serializable
private data class GroqResponse(val choices: List<GroqChoice> = emptyList())

@Serializable
private data class RecommendationsWrapper(
    val recommendations: List<RawRecommendation> = emptyList(),
)

class GroqRecommendationDataSource(
    private val httpClient: HttpClient,
    private val config: GroqConfig,
) : RecommendationDataSource {
    override suspend fun getRawRecommendations(
        collection: List<CollectionEntry>,
    ): Result<List<RawRecommendation>, RecommendationError> {
        val prompt = buildPrompt(collection)
        val requestBody =
            GroqRequest(
                model = GROQ_MODEL,
                messages = listOf(GroqMessage(role = "user", content = prompt)),
                responseFormat = GroqResponseFormat(type = "json_object"),
            )

        return try {
            val response =
                httpClient.post(GROQ_BASE_URL) {
                    header("Authorization", "Bearer ${config.apiKey}")
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(GroqRequest.serializer(), requestBody))
                }

            if (response.status != HttpStatusCode.OK) {
                val body = response.bodyAsText()
                return Result.Failure(
                    RecommendationError.NetworkError(
                        RuntimeException("HTTP ${response.status.value}: $body")
                    )
                )
            }

            val groqResponse: GroqResponse = json.decodeFromString(response.bodyAsText())
            val text =
                groqResponse.choices.firstOrNull()?.message?.content
                    ?: return Result.Failure(RecommendationError.NoResults)

            val recommendations = json.decodeFromString<RecommendationsWrapper>(text).recommendations
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
