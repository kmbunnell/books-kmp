package com.example.books_kmp.domain.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Book

interface RecommendationRepository {
    suspend fun getRecommendations(books: List<Book>): Result<List<BookRecommendation>, RecommendationError>
}
