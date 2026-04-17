package com.example.books_kmp.ui.addbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_add
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_look_up
import bookskmp.composeapp.generated.resources.button_ok
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.cd_book_cover
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.error_duplicate_message
import bookskmp.composeapp.generated.resources.error_duplicate_title
import bookskmp.composeapp.generated.resources.error_network_generic
import bookskmp.composeapp.generated.resources.error_rate_limited
import bookskmp.composeapp.generated.resources.label_is_this_right_book
import bookskmp.composeapp.generated.resources.label_isbn
import bookskmp.composeapp.generated.resources.title_add_book
import coil3.compose.AsyncImage
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.AddBookEffect
import com.example.books_kmp.viewmodel.AddBookIntent
import com.example.books_kmp.viewmodel.AddBookScreenError
import com.example.books_kmp.viewmodel.AddBookUiState
import com.example.books_kmp.viewmodel.AddBookViewModel
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddBookScreen(
    onNavigateUp: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
) {
    val viewModel: AddBookViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    AddBookScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateToManualEntry = onNavigateToManualEntry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreenContent(
    uiState: AddBookUiState,
    effects: SharedFlow<AddBookEffect>,
    onIntent: (AddBookIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
) {
    val errorNetworkGeneric = stringResource(Res.string.error_network_generic)
    val errorRateLimited = stringResource(Res.string.error_rate_limited)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                AddBookEffect.NavigateToLibrary -> onNavigateToLibrary()
                AddBookEffect.NavigateToManualEntry -> onNavigateToManualEntry()
            }
        }
    }

    if (uiState.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.error_duplicate_title)) },
            text = { Text(stringResource(Res.string.error_duplicate_message)) },
            confirmButton = {
                Button(
                    onClick = { onIntent(AddBookIntent.DismissDuplicateDialog) },
                    modifier = Modifier.testTag(TestTags.AddBook.DuplicateDialogOkButton),
                ) {
                    Text(stringResource(Res.string.button_ok))
                }
            },
            modifier = Modifier.testTag(TestTags.AddBook.DuplicateDialog),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_add_book)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.testTag(TestTags.AddBook.NavigateUpButton),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.isbn,
                onValueChange = { if (it.length <= 13) onIntent(AddBookIntent.IsbnChanged(it)) },
                label = { Text(stringResource(Res.string.label_isbn)) },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.AddBook.IsbnField),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag(TestTags.AddBook.LoadingIndicator))
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { onIntent(AddBookIntent.LookupIsbn(uiState.isbn)) },
                enabled = !uiState.isLoading && uiState.isbn.isNotBlank(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.AddBook.LookUpButton),
            ) {
                Text(stringResource(Res.string.button_look_up))
            }

            uiState.error?.let { error ->
                val message =
                    when (error) {
                        AddBookScreenError.NetworkError -> errorNetworkGeneric
                        AddBookScreenError.RateLimited -> errorRateLimited
                    }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    modifier = Modifier.testTag(TestTags.AddBook.NetworkErrorBanner),
                )
                TextButton(
                    onClick = { onIntent(AddBookIntent.Retry) },
                    modifier = Modifier.testTag(TestTags.AddBook.RetryButton),
                ) {
                    Text(stringResource(Res.string.button_retry))
                }
            }

            uiState.foundBook?.let { book ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(Res.string.label_is_this_right_book))
                book.coverImageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = stringResource(Res.string.cd_book_cover),
                        modifier = Modifier.testTag(TestTags.AddBook.BookPreviewCover),
                    )
                }
                Text(
                    text = book.title,
                    modifier = Modifier.testTag(TestTags.AddBook.BookPreviewTitle),
                )
                Text(
                    text = book.authors.joinToString(", "),
                    modifier = Modifier.testTag(TestTags.AddBook.BookPreviewAuthors),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onIntent(AddBookIntent.ConfirmBook) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.AddBook.AddButton),
                ) {
                    Text(stringResource(Res.string.button_add))
                }
                TextButton(
                    onClick = { onIntent(AddBookIntent.CancelBookPreview) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.AddBook.CancelButton),
                ) {
                    Text(stringResource(Res.string.button_cancel))
                }
            }
        }
    }
}
