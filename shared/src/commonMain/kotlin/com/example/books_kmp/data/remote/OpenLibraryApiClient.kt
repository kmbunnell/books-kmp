package com.example.books_kmp.data.remote

import com.example.books_kmp.data.remote.dto.OpenLibraryBookDto
import com.example.books_kmp.domain.model.BookLookupData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://openlibrary.org/api/books"
private const val USER_AGENT = "Shelved/1.0"

private val json = Json { ignoreUnknownKeys = true }

class OpenLibraryApiClient(private val httpClient: HttpClient) {
    suspend fun lookupByIsbn(isbn: String): OpenLibraryResult =
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
                HttpStatusCode.TooManyRequests -> OpenLibraryResult.RateLimited
                HttpStatusCode.OK -> parseBody(isbn, response.bodyAsText())
                else -> OpenLibraryResult.NetworkError(
                    RuntimeException("Unexpected HTTP ${response.status.value}"),
                )
            }
        } catch (e: SerializationException) {
            OpenLibraryResult.MalformedResponse
        } catch (e: Exception) {
            OpenLibraryResult.NetworkError(e)
        }

    private fun parseBody(isbn: String, bodyText: String): OpenLibraryResult {
        val responseMap: Map<String, OpenLibraryBookDto> =
            json.decodeFromString(bodyText)
        val dto = responseMap["ISBN:$isbn"] ?: return OpenLibraryResult.NotFound
        val title = dto.title ?: return OpenLibraryResult.MalformedResponse
        return OpenLibraryResult.Found(
            bookData =
                BookLookupData(
                    isbn = isbn,
                    title = title,
                    authors = dto.authors?.mapNotNull { it.name } ?: emptyList(),
                    coverImageUrl = dto.cover?.medium,
                ),
        )
    }
}
