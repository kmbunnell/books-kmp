package com.example.books_kmp.ui.recommendations

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.books_kmp.domain.recommendation.BookRecommendation
import com.example.books_kmp.domain.recommendation.RecommendationError
import com.example.books_kmp.ui.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RecommendationsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val recommendation =
        BookRecommendation(
            title = "Recommended Book",
            authors = listOf("Other Author"),
            isbn = "222",
            coverUrl = "http://cover",
            reason = "Because you liked Seed Book",
            description = "A description",
        )

    @Test
    fun `loading state shows loading indicator`() {
        composeTestRule.setContent {
            RecommendationsScreenContent(
                uiState = RecommendationsUiState.Loading,
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Recommendations.LoadingIndicator).assertIsDisplayed()
    }

    @Test
    fun `success state shows recommendation list`() {
        composeTestRule.setContent {
            RecommendationsScreenContent(
                uiState = RecommendationsUiState.Success(listOf(recommendation)),
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Recommendations.List).assertIsDisplayed()
    }

    @Test
    fun `error state shows error message`() {
        composeTestRule.setContent {
            RecommendationsScreenContent(
                uiState = RecommendationsUiState.Error(RecommendationError.NoResults),
                onNavigateUp = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Recommendations.ErrorMessage).assertIsDisplayed()
    }
}
