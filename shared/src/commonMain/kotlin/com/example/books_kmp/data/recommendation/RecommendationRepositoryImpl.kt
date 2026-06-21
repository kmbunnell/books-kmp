package com.example.books_kmp.data.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.recommendation.BookRecommendation
import com.example.books_kmp.domain.recommendation.RecommendationError
import com.example.books_kmp.domain.recommendation.RecommendationRepository

class RecommendationRepositoryImpl(
    private val dataSource: RecommendationDataSource,
) : RecommendationRepository {
    override suspend fun getRecommendations(books: List<Book>): Result<List<BookRecommendation>, RecommendationError> {
        val collection =
            books.map { book ->
                CollectionEntry(
                    title = book.title,
                    authors = book.authors,
                )
            }
        return when (val result = dataSource.getRawRecommendations(collection)) {
            is Result.Success ->
                Result.Success(
                    result.data.map { raw ->
                        BookRecommendation(
                            title = raw.title,
                            authors = raw.authors,
                            isbn = null,
                            coverUrl = null,
                            reason = raw.reason,
                            description = raw.description ?: "",
                        )
                    },
                )
            is Result.Failure -> result
        }
    }
}
