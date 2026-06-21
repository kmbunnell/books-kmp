package com.example.books_kmp.ui.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.action_retry
import bookskmp.composeapp.generated.resources.cd_book_cover
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.recommendations_error_network
import bookskmp.composeapp.generated.resources.recommendations_error_no_results
import bookskmp.composeapp.generated.resources.recommendations_error_not_premium
import bookskmp.composeapp.generated.resources.recommendations_error_unauthenticated
import bookskmp.composeapp.generated.resources.recommendations_title
import com.example.books_kmp.domain.recommendation.BookRecommendation
import com.example.books_kmp.domain.recommendation.RecommendationError
import com.example.books_kmp.ui.BookCoverImage
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecommendationsScreen(
    tagIds: List<String>,
    onNavigateUp: () -> Unit,
) {
    val viewModel: RecommendationsViewModel = koinViewModel(parameters = { parametersOf(tagIds) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendationsScreenContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecommendationsScreenContent(
    uiState: RecommendationsUiState,
    onNavigateUp: () -> Unit,
    onIntent: (RecommendationsIntent) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.recommendations_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            RecommendationsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag(TestTags.Recommendations.LoadingIndicator),
                    )
                }
            }
            is RecommendationsUiState.Success -> {
                RecommendationsList(
                    recommendations = uiState.recommendations,
                    contentPadding = innerPadding,
                )
            }
            is RecommendationsUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = errorMessage(uiState.error),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(TestTags.Recommendations.ErrorMessage),
                    )
                    if (uiState.error is RecommendationError.NetworkError) {
                        Button(
                            onClick = { onIntent(RecommendationsIntent.Retry) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsList(
    recommendations: List<BookRecommendation>,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(TestTags.Recommendations.List),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recommendations) { recommendation ->
            RecommendationCard(recommendation)
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: BookRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BookCoverImage(
                url = recommendation.coverUrl,
                contentDescription = stringResource(Res.string.cd_book_cover),
                modifier = Modifier.height(120.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (recommendation.authors.isNotEmpty()) {
                    Text(
                        text = recommendation.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun errorMessage(error: RecommendationError): String =
    when (error) {
        RecommendationError.NoResults -> stringResource(Res.string.recommendations_error_no_results)
        is RecommendationError.NetworkError -> stringResource(Res.string.recommendations_error_network)
        RecommendationError.NotPremium -> stringResource(Res.string.recommendations_error_not_premium)
        RecommendationError.Unauthenticated -> stringResource(Res.string.recommendations_error_unauthenticated)
    }
