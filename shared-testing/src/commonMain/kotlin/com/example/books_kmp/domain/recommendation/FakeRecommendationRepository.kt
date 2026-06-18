package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book

class FakeRecommendationRepository : RecommendationRepository {
    var nextResult: Result<List<BookRecommendation>, RecommendationError> = Result.Success(emptyList())
    var lastSeedBooks: List<Book>? = null
    var getRecommendationsCalled = 0

    override suspend fun getRecommendations(books: List<Book>): Result<List<BookRecommendation>, RecommendationError> {
        getRecommendationsCalled++
        lastSeedBooks = books
        return nextResult
    }
}
