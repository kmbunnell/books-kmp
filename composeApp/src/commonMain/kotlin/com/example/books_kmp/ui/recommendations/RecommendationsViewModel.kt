package com.example.books_kmp.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.recommendation.BookRecommendation
import com.example.books_kmp.domain.recommendation.GetRecommendationsUseCase
import com.example.books_kmp.domain.recommendation.RecommendationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecommendationsUiState {
    data object Loading : RecommendationsUiState

    data class Success(val recommendations: List<BookRecommendation>) : RecommendationsUiState

    data class Error(val error: RecommendationError) : RecommendationsUiState
}

sealed interface RecommendationsIntent {
    data object Load : RecommendationsIntent
    data object Retry : RecommendationsIntent
}

class RecommendationsViewModel(
    private val tagIds: List<String>,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RecommendationsUiState>(RecommendationsUiState.Loading)
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        onIntent(RecommendationsIntent.Load)
    }

    fun onIntent(intent: RecommendationsIntent) {
        when (intent) {
            RecommendationsIntent.Load -> load()
            RecommendationsIntent.Retry -> {
                _uiState.value = RecommendationsUiState.Loading
                load()
            }
        }
    }

    private fun load() {
        if (_uiState.value !is RecommendationsUiState.Loading) return
        viewModelScope.launch {
            _uiState.value =
                when (val result = getRecommendationsUseCase(tagIds)) {
                    is Result.Success -> RecommendationsUiState.Success(result.data)
                    is Result.Failure -> RecommendationsUiState.Error(result.error)
                }
        }
    }
}
