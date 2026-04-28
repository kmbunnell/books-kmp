package com.example.books_kmp.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_manage_tags
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.error_book_detail_load_failed
import bookskmp.composeapp.generated.resources.error_tag_operation_failed
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.BookDetailIntent
import com.example.books_kmp.viewmodel.BookDetailUiState
import com.example.books_kmp.viewmodel.BookDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BookDetailScreen(
    bookId: String,
    onNavigateUp: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
) {
    val viewModel: BookDetailViewModel = koinViewModel(parameters = { parametersOf(bookId) })
    val uiState by viewModel.uiState.collectAsState()
    BookDetailScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onNavigateToTagManagement = onNavigateToTagManagement,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BookDetailScreenContent(
    uiState: BookDetailUiState,
    onIntent: (BookDetailIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(Res.string.error_tag_operation_failed)
    val loadFailedMessage = stringResource(Res.string.error_book_detail_load_failed)
    val retryLabel = stringResource(Res.string.button_retry)

    LaunchedEffect(uiState.tagToggleError) {
        if (uiState.tagToggleError != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onIntent(BookDetailIntent.DismissTagToggleError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(TestTags.BookDetail.LoadingIndicator),
                    )
                }
                uiState.loadFailed -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = loadFailedMessage,
                            modifier = Modifier.testTag(TestTags.BookDetail.LoadFailedMessage),
                        )
                        Button(
                            onClick = { onIntent(BookDetailIntent.Reload) },
                            modifier = Modifier.testTag(TestTags.BookDetail.RetryButton),
                        ) {
                            Text(retryLabel)
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.allTags.forEach { tag ->
                                FilterChip(
                                    selected = tag.id in uiState.appliedTagIds,
                                    enabled = tag.id !in uiState.inFlightTagIds,
                                    onClick = { onIntent(BookDetailIntent.ToggleTag(tag.id)) },
                                    label = { Text(tag.name) },
                                    modifier = Modifier.testTag(TestTags.BookDetail.tagChip(tag.id)),
                                )
                            }
                        }
                        TextButton(
                            onClick = onNavigateToTagManagement,
                            modifier = Modifier.testTag(TestTags.BookDetail.ManageTagsButton),
                        ) {
                            Text(stringResource(Res.string.button_manage_tags))
                        }
                    }
                }
            }
        }
    }
}
