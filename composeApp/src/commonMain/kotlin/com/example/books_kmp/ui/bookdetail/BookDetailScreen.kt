package com.example.books_kmp.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_confirm_delete
import bookskmp.composeapp.generated.resources.button_manage_tags
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.cd_book_cover
import bookskmp.composeapp.generated.resources.cd_delete_book
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.error_book_detail_load_failed
import bookskmp.composeapp.generated.resources.error_delete_book_failed
import bookskmp.composeapp.generated.resources.error_tag_operation_failed
import bookskmp.composeapp.generated.resources.message_delete_book
import bookskmp.composeapp.generated.resources.title_book_detail
import bookskmp.composeapp.generated.resources.title_delete_book
import com.example.books_kmp.ui.BookCoverImage
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import com.example.books_kmp.ui.components.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BookDetailScreen(
    bookId: String,
    onNavigateUp: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
    isAdaptiveDetail: Boolean = false,
) {
    val viewModel: BookDetailViewModel = koinViewModel(key = bookId, parameters = { parametersOf(bookId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorTagMessage = stringResource(Res.string.error_tag_operation_failed)
    val errorDeleteMessage = stringResource(Res.string.error_delete_book_failed)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BookDetailEffect.NavigateUp -> onNavigateUp()
                is BookDetailEffect.ShowError -> {
                    val message =
                        when (effect.error) {
                            BookDetailError.ToggleFailed -> errorTagMessage
                            BookDetailError.DeleteFailed -> errorDeleteMessage
                        }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    BookDetailScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onNavigateToTagManagement = onNavigateToTagManagement,
        snackbarHostState = snackbarHostState,
        isAdaptiveDetail = isAdaptiveDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BookDetailScreenContent(
    uiState: BookDetailUiState,
    onIntent: (BookDetailIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
    snackbarHostState: SnackbarHostState,
    isAdaptiveDetail: Boolean = false,
) {
    val loadFailedMessage = stringResource(Res.string.error_book_detail_load_failed)
    val retryLabel = stringResource(Res.string.button_retry)
    val titleDeleteBook = stringResource(Res.string.title_delete_book)
    val messageDeleteBook = stringResource(Res.string.message_delete_book)
    val buttonConfirmDelete = stringResource(Res.string.button_confirm_delete)
    val buttonCancel = stringResource(Res.string.button_cancel)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_book_detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onIntent(BookDetailIntent.DeleteBook) },
                        enabled = !uiState.isLoading && !uiState.loadFailed && !uiState.isDeleting,
                        modifier = Modifier.testTag(TestTags.BookDetail.DeleteButton),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.cd_delete_book),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading || uiState.isDeleting -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
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
                            style = MaterialTheme.typography.bodyLarge,
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        ) {
                            BookCoverImage(
                                url = uiState.book?.coverImageUrl,
                                contentDescription = stringResource(Res.string.cd_book_cover),
                                modifier =
                                    Modifier
                                        .height(if (isAdaptiveDetail) 300.dp else 200.dp)
                                        .testTag(TestTags.BookDetail.CoverImage),
                            )
                        }
                        Column(
                            modifier =
                                Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(modifier = Modifier.wrapContentWidth()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        maxItemsInEachRow = 4,
                                    ) {
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
        }

        if (uiState.showDeleteConfirm) {
            ConfirmationDialog(
                title = titleDeleteBook,
                message = messageDeleteBook,
                confirmLabel = buttonConfirmDelete,
                dismissLabel = buttonCancel,
                onConfirm = { onIntent(BookDetailIntent.ConfirmDelete) },
                onDismiss = { onIntent(BookDetailIntent.DismissDelete) },
            )
        }
    }
}
