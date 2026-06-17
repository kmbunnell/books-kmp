package com.example.books_kmp.data.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.recommendation.RecommendationError

interface RecommendationDataSource {
    suspend fun getRawRecommendations(
        collection: List<CollectionEntry>,
    ): Result<List<RawRecommendation>, RecommendationError>
}
