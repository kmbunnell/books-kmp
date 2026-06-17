package com.example.books_kmp.data.recommendation

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.recommendation.RecommendationError

class FakeRecommendationDataSource(
    var nextResult: Result<List<RawRecommendation>, RecommendationError> =
        Result.Success(emptyList()),
) : RecommendationDataSource {
    var lastCollection: List<CollectionEntry>? = null
    var getRawRecommendationsCalled = 0

    override suspend fun getRawRecommendations(
        collection: List<CollectionEntry>,
    ): Result<List<RawRecommendation>, RecommendationError> {
        getRawRecommendationsCalled++
        lastCollection = collection
        return nextResult
    }
}
