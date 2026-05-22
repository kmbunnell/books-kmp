package com.example.books_kmp.data.remote

import com.example.books_kmp.data.remote.dto.EdgeFunctionBookDto
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val LOOKUP_FUNCTION_PATH = "functions/v1/lookup-book"

private val json = Json { ignoreUnknownKeys = true }

class GoogleBooksApiClient(
    private val httpClient: HttpClient,
    private val accessTokenProvider: () -> String?,
    private val supabaseUrl: String,
) : BookLookupService {
    private val functionUrl = "${supabaseUrl.trimEnd('/')}/$LOOKUP_FUNCTION_PATH"

    override suspend fun lookupByIsbn(isbn: String): Result<BookLookupData, BookLookupError> {
        val token =
            accessTokenProvider()
                ?: return Result.Failure(BookLookupError.Unauthenticated)

        return try {
            val response =
                postLookup(
                    token = token,
                    body =
                        buildJsonObject {
                            put("type", JsonPrimitive("isbn"))
                            put("isbn", JsonPrimitive(isbn))
                        },
                )

            when (response.status) {
                HttpStatusCode.OK -> {
                    val dto: EdgeFunctionBookDto = json.decodeFromString(response.bodyAsText())
                    Result.Success(dto.toDomain())
                }
                HttpStatusCode.NotFound -> Result.Failure(BookLookupError.NotFound)
                HttpStatusCode.Unauthorized -> Result.Failure(BookLookupError.Unauthenticated)
                HttpStatusCode.TooManyRequests -> Result.Failure(BookLookupError.RateLimited)
                else -> Result.Failure(BookLookupError.NetworkError)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.Failure(BookLookupError.MalformedResponse)
        } catch (e: Exception) {
            Result.Failure(BookLookupError.NetworkError)
        }
    }

    override suspend fun lookupByTitle(title: String): Result<List<BookLookupData>, BookLookupError> {
        val token =
            accessTokenProvider()
                ?: return Result.Failure(BookLookupError.Unauthenticated)

        return try {
            val response =
                postLookup(
                    token = token,
                    body =
                        buildJsonObject {
                            put("type", JsonPrimitive("title"))
                            put("query", JsonPrimitive(title))
                        },
                )

            when (response.status) {
                HttpStatusCode.OK -> {
                    val dtos: List<EdgeFunctionBookDto> = json.decodeFromString(response.bodyAsText())
                    Result.Success(dtos.map { it.toDomain() })
                }
                HttpStatusCode.NotFound -> Result.Failure(BookLookupError.NotFound)
                HttpStatusCode.Unauthorized -> Result.Failure(BookLookupError.Unauthenticated)
                HttpStatusCode.TooManyRequests -> Result.Failure(BookLookupError.RateLimited)
                else -> Result.Failure(BookLookupError.NetworkError)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.Failure(BookLookupError.MalformedResponse)
        } catch (e: Exception) {
            Result.Failure(BookLookupError.NetworkError)
        }
    }

    private suspend fun postLookup(
        token: String,
        body: JsonObject,
    ): HttpResponse =
        httpClient.post(functionUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(JsonObject.serializer(), body))
        }
}

private fun EdgeFunctionBookDto.toDomain(): BookLookupData =
    BookLookupData(
        isbn = isbn,
        title = title,
        authors = authors,
        coverImageUrl = coverUrl,
    )
