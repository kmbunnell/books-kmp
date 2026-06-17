package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.data.recommendation.FakeRecommendationDataSource
import com.example.books_kmp.data.recommendation.RawRecommendation
import com.example.books_kmp.data.recommendation.RecommendationRepositoryImpl
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class RecommendationRepositoryImplTest {
    private val books =
        listOf(
            Book(
                id = "1",
                isbn = "111",
                title = "Dune",
                authors = listOf("Frank Herbert"),
                coverImageUrl = "url",
            ),
        )

    @Test
    fun `returns mapped recommendations on success`() =
        runTest {
            val dataSource =
                FakeRecommendationDataSource(
                    nextResult =
                        Result.Success(
                            listOf(
                                RawRecommendation(
                                    title = "Hyperion",
                                    authors = listOf("Dan Simmons"),
                                    reason = "Epic sci-fi",
                                    description = "A space opera",
                                ),
                            ),
                        ),
                )
            val repository = RecommendationRepositoryImpl(dataSource)

            val result = repository.getRecommendations(books)

            val success = assertIs<Result.Success<List<BookRecommendation>>>(result)
            assertEquals(1, success.data.size)
            val recommendation = success.data.first()
            assertEquals("Hyperion", recommendation.title)
            assertEquals(listOf("Dan Simmons"), recommendation.authors)
            assertEquals("Epic sci-fi", recommendation.reason)
            assertEquals("A space opera", recommendation.description)
            assertNull(recommendation.isbn)
            assertNull(recommendation.coverUrl)
        }

    @Test
    fun `maps null description to empty string`() =
        runTest {
            val dataSource =
                FakeRecommendationDataSource(
                    nextResult =
                        Result.Success(
                            listOf(
                                RawRecommendation(
                                    title = "Hyperion",
                                    authors = listOf("Dan Simmons"),
                                    reason = "Epic sci-fi",
                                    description = null,
                                ),
                            ),
                        ),
                )
            val repository = RecommendationRepositoryImpl(dataSource)

            val result = repository.getRecommendations(books)

            val success = assertIs<Result.Success<List<BookRecommendation>>>(result)
            assertEquals("", success.data.first().description)
        }

    @Test
    fun `propagates NetworkError failure`() =
        runTest {
            val cause = RuntimeException("boom")
            val dataSource =
                FakeRecommendationDataSource(
                    nextResult = Result.Failure(RecommendationError.NetworkError(cause)),
                )
            val repository = RecommendationRepositoryImpl(dataSource)

            val result = repository.getRecommendations(books)

            val failure = assertIs<Result.Failure<RecommendationError>>(result)
            val error = assertIs<RecommendationError.NetworkError>(failure.error)
            assertEquals(cause, error.cause)
        }

    @Test
    fun `propagates NoResults failure`() =
        runTest {
            val dataSource =
                FakeRecommendationDataSource(
                    nextResult = Result.Failure(RecommendationError.NoResults),
                )
            val repository = RecommendationRepositoryImpl(dataSource)

            val result = repository.getRecommendations(books)

            val failure = assertIs<Result.Failure<RecommendationError>>(result)
            assertEquals(RecommendationError.NoResults, failure.error)
        }

    @Test
    fun `maps books to collection entries with only title and authors`() =
        runTest {
            val dataSource =
                FakeRecommendationDataSource(
                    nextResult = Result.Success(emptyList()),
                )
            val repository = RecommendationRepositoryImpl(dataSource)

            repository.getRecommendations(books)

            val collection = dataSource.lastCollection
            assertEquals(1, collection?.size)
            assertEquals("Dune", collection?.first()?.title)
            assertEquals(listOf("Frank Herbert"), collection?.first()?.authors)
        }
}
