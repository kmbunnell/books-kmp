package com.example.books_kmp.data.remote

import com.example.books_kmp.data.remote.dto.OpenLibraryBookDto
import com.example.books_kmp.data.remote.dto.OpenLibrarySearchResponseDto
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookLookupService
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.domain.model.BookLookupError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://openlibrary.org/api/books"
private const val SEARCH_URL = "https://openlibrary.org/search.json"
private const val USER_AGENT = "Shelved/1.0"

private const val COVER_BASE_URL = "https://covers.openlibrary.org/b/id"

private val json = Json { ignoreUnknownKeys = true }

private fun coverImageUrl(coverId: Int?): String? {
    return if (coverId != null) "$COVER_BASE_URL/$coverId-M.jpg" else null
}

class OpenLibraryApiClient(private val httpClient: HttpClient) : BookLookupService {
    override suspend fun lookupByIsbn(isbn: String): Result<BookLookupData, BookLookupError> =
        try {
            val response =
                httpClient.get(BASE_URL) {
                    url {
                        parameters.append("bibkeys", "ISBN:$isbn")
                        parameters.append("format", "json")
                        parameters.append("jscmd", "data")
                    }
                    header(HttpHeaders.UserAgent, USER_AGENT)
                }

            when (response.status) {
                HttpStatusCode.TooManyRequests -> Result.Failure(BookLookupError.RateLimited)
                HttpStatusCode.OK -> parseBody(isbn, response.bodyAsText())
                else -> Result.Failure(BookLookupError.NetworkError)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.Failure(BookLookupError.MalformedResponse)
        } catch (e: Exception) {
            Result.Failure(BookLookupError.NetworkError)
        }

    override suspend fun lookupByTitle(title: String): Result<List<BookLookupData>, BookLookupError> =
        try {
            val response =
                httpClient.get(SEARCH_URL) {
                    url {
                        parameters.append("title", title)
                        parameters.append("limit", "10")
                    }
                    header(HttpHeaders.UserAgent, USER_AGENT)
                }

            when (response.status) {
                HttpStatusCode.TooManyRequests -> Result.Failure(BookLookupError.RateLimited)
                HttpStatusCode.OK -> {
                    val dto: OpenLibrarySearchResponseDto = json.decodeFromString(response.bodyAsText())
                    if (dto.docs.isEmpty()) {
                        Result.Failure(BookLookupError.NotFound)
                    } else {
                        Result.Success(
                            dto.docs.map { doc ->
                                BookLookupData(
                                    isbn = null,
                                    title = doc.title,
                                    authors = doc.authorName,
                                    coverImageUrl = coverImageUrl(doc.coverId),
                                )
                            },
                        )
                    }
                }
                else ->
                    Result.Failure(BookLookupError.NetworkError)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.Failure(BookLookupError.MalformedResponse)
        } catch (e: Exception) {
            Result.Failure(BookLookupError.NetworkError)
        }

    private fun parseBody(
        isbn: String,
        bodyText: String
    ): Result<BookLookupData, BookLookupError> {
        val responseMap: Map<String, OpenLibraryBookDto> =
            json.decodeFromString(bodyText)
        val dto = responseMap["ISBN:$isbn"] ?: return Result.Failure(BookLookupError.NotFound)
        val title = dto.title ?: return Result.Failure(BookLookupError.MalformedResponse)
        return Result.Success(
            BookLookupData(
                isbn = isbn,
                title = title,
                authors = dto.authors?.mapNotNull { it.name } ?: emptyList(),
                coverImageUrl = dto.cover?.medium,
            ),
        )
    }
}
