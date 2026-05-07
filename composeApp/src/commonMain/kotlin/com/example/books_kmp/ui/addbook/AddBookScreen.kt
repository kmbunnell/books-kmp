package com.example.books_kmp.ui.addbook

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_add
import bookskmp.composeapp.generated.resources.button_add_anyway
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_enter_manually
import bookskmp.composeapp.generated.resources.button_look_up
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.camera_permission_permanently_denied
import bookskmp.composeapp.generated.resources.cd_book_cover
import bookskmp.composeapp.generated.resources.cd_close
import bookskmp.composeapp.generated.resources.cd_close_scanner
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.cd_scan_barcode
import bookskmp.composeapp.generated.resources.error_duplicate_message
import bookskmp.composeapp.generated.resources.error_duplicate_title
import bookskmp.composeapp.generated.resources.error_isbn_not_found
import bookskmp.composeapp.generated.resources.error_network_generic
import bookskmp.composeapp.generated.resources.error_rate_limited
import bookskmp.composeapp.generated.resources.error_scan_failed
import bookskmp.composeapp.generated.resources.error_unavailable_hardware
import bookskmp.composeapp.generated.resources.label_isbn
import bookskmp.composeapp.generated.resources.label_isbn_search
import bookskmp.composeapp.generated.resources.label_title_search
import bookskmp.composeapp.generated.resources.title_results_enter_manually
import bookskmp.composeapp.generated.resources.snackbar_book_added
import bookskmp.composeapp.generated.resources.title_add_book
import coil3.compose.AsyncImage
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.scan.BarcodeScannerView
import com.example.books_kmp.viewmodel.AddBookEffect
import com.example.books_kmp.viewmodel.AddBookIntent
import com.example.books_kmp.viewmodel.AddBookScreenError
import com.example.books_kmp.viewmodel.AddBookUiState
import com.example.books_kmp.viewmodel.AddBookViewModel
import com.example.books_kmp.viewmodel.LookupMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddBookScreen(
    onNavigateUp: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
) {
    val viewModel: AddBookViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    AddBookScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
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
    onNavigateToManualEntry: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarBookAdded = stringResource(Res.string.snackbar_book_added)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        effects.collect { effect ->
            when (effect) {
                AddBookEffect.BookAdded -> snackbarHostState.showSnackbar(snackbarBookAdded)
                AddBookEffect.NavigateToManualEntry -> onNavigateToManualEntry()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(Res.string.error_duplicate_title)) },
                text = { Text(stringResource(Res.string.error_duplicate_message)) },
                confirmButton = {
                    Button(
                        onClick = { onIntent(AddBookIntent.AddAnyway) },
                        modifier = Modifier.testTag(TestTags.AddBook.DuplicateDialogAddAnywayButton),
                    ) {
                        Text(stringResource(Res.string.button_add_anyway))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onIntent(AddBookIntent.DismissDuplicateDialog) },
                        modifier = Modifier.testTag(TestTags.AddBook.DuplicateDialogCancelButton),
                    ) {
                        Text(stringResource(Res.string.button_cancel))
                    }
                },
                modifier = Modifier.testTag(TestTags.AddBook.DuplicateDialog),
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
            LazyColumn(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    val isbnLabel = stringResource(Res.string.label_isbn_search)
                    val titleLabel = stringResource(Res.string.label_title_search)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.AddBook.LookupModeToggle),
                    ) {
                        LookupMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.lookupMode == mode,
                                onClick = { onIntent(AddBookIntent.SetLookupMode(mode)) },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = LookupMode.entries.size,
                                    ),
                                label = {
                                    Text(if (mode == LookupMode.ISBN) isbnLabel else titleLabel)
                                },
                                modifier =
                                    Modifier.testTag(
                                        if (mode == LookupMode.ISBN) {
                                            TestTags.AddBook.IsbnModeButton
                                        } else {
                                            TestTags.AddBook.TitleModeButton
                                        },
                                    ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LookupModeSection(
                        lookupMode = uiState.lookupMode,
                        isbn = uiState.isbn,
                        titleQuery = uiState.titleQuery,
                        isLoading = uiState.isLoading,
                        focusRequester = focusRequester,
                        onIntent = onIntent,
                    )
                }

                itemsIndexed(uiState.titleResults) { index, book ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = { onIntent(AddBookIntent.SelectTitleResult(book)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.AddBook.titleResultItem(index)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = book.title)
                            Text(text = book.authors.joinToString(", "))
                        }
                    }
                }

                if (uiState.titleResults.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { onIntent(AddBookIntent.EnterManually) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.AddBook.EnterManuallyButton),
                        ) {
                            Text(stringResource(Res.string.title_results_enter_manually))
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.testTag(TestTags.AddBook.LoadingIndicator),
                            )
                        }
                    }
                }

                uiState.error?.let { error ->
                    item {
                        ErrorSection(error = error, onIntent = onIntent)
                    }
                }

                uiState.foundBook?.let { book ->
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                ) {
                                    BookCoverImage(
                                        url = book.coverImageUrl,
                                        contentDescription = stringResource(Res.string.cd_book_cover),
                                        modifier =
                                            Modifier
                                                .size(120.dp)
                                                .testTag(TestTags.AddBook.BookPreviewCover),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                }
                                IconButton(
                                    onClick = { onIntent(AddBookIntent.CancelBookPreview) },
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .testTag(TestTags.AddBook.CancelButton),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(Res.string.cd_close),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isScanning) {
            Box(modifier = Modifier.fillMaxSize()) {
                BarcodeScannerView(
                    onResult = { result ->
                        when (result) {
                            is Result.Success -> onIntent(AddBookIntent.BarcodeScanned(result.data))
                            is Result.Failure -> onIntent(AddBookIntent.ScanFailed(result.error))
                        }
                    },
                )
                IconButton(
                    onClick = { onIntent(AddBookIntent.ScanDismissed) },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cd_close_scanner),
                    )
                }
            }
        }
    }
}

@Composable
private fun LookupModeSection(
    lookupMode: LookupMode,
    isbn: String,
    titleQuery: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onIntent: (AddBookIntent) -> Unit,
) {
    if (lookupMode == LookupMode.ISBN) {
        OutlinedTextField(
            value = isbn,
            onValueChange = { if (it.length <= 13) onIntent(AddBookIntent.IsbnChanged(it)) },
            label = { Text(stringResource(Res.string.label_isbn)) },
            singleLine = true,
            enabled = !isLoading,
            trailingIcon = {
                IconButton(
                    onClick = { onIntent(AddBookIntent.StartScan) },
                    modifier = Modifier.testTag(TestTags.AddBook.ScanButton),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(Res.string.cd_scan_barcode),
                    )
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(TestTags.AddBook.IsbnField),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onIntent(AddBookIntent.LookupIsbn(isbn)) },
            enabled = !isLoading && isbn.isNotBlank(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AddBook.LookUpButton),
        ) {
            Text(stringResource(Res.string.button_look_up))
        }
    } else {
        OutlinedTextField(
            value = titleQuery,
            onValueChange = { onIntent(AddBookIntent.TitleChanged(it)) },
            label = { Text(stringResource(Res.string.label_title_search)) },
            singleLine = true,
            enabled = !isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AddBook.TitleField),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onIntent(AddBookIntent.LookupByTitle(titleQuery)) },
            enabled = !isLoading && titleQuery.isNotBlank(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AddBook.LookUpButton),
        ) {
            Text(stringResource(Res.string.button_look_up))
        }
    }
}

@Composable
private fun BookCoverImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var usePlaceholder by remember(url) { mutableStateOf(url == null) }
    if (usePlaceholder) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.border(1.dp, Color.Gray, RoundedCornerShape(5))
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            onError = { usePlaceholder = true },
        )
    }
}

@Composable
private fun ErrorSection(
    error: AddBookScreenError,
    onIntent: (AddBookIntent) -> Unit,
) {
    val message =
        stringResource(
            when (error) {
                AddBookScreenError.NotFound -> Res.string.error_isbn_not_found
                AddBookScreenError.NetworkError -> Res.string.error_network_generic
                AddBookScreenError.RateLimited -> Res.string.error_rate_limited
                AddBookScreenError.ScanCameraPermissionDenied -> Res.string.camera_permission_permanently_denied
                AddBookScreenError.ScanHardwareUnavailable -> Res.string.error_unavailable_hardware
                AddBookScreenError.ScanUnknownError -> Res.string.error_scan_failed
            },
        )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        modifier = Modifier.testTag(TestTags.AddBook.ErrorBanner),
    )
    when (error) {
        AddBookScreenError.NotFound ->
            TextButton(
                onClick = { onIntent(AddBookIntent.EnterManually) },
                modifier = Modifier.testTag(TestTags.AddBook.EnterManuallyButton),
            ) {
                Text(stringResource(Res.string.button_enter_manually))
            }
        AddBookScreenError.NetworkError,
        AddBookScreenError.RateLimited ->
            TextButton(
                onClick = { onIntent(AddBookIntent.Retry) },
                modifier = Modifier.testTag(TestTags.AddBook.RetryButton),
            ) {
                Text(stringResource(Res.string.button_retry))
            }
        AddBookScreenError.ScanCameraPermissionDenied,
        AddBookScreenError.ScanHardwareUnavailable -> Unit
        AddBookScreenError.ScanUnknownError ->
            TextButton(
                onClick = { onIntent(AddBookIntent.StartScan) },
                modifier = Modifier.testTag(TestTags.AddBook.RetryButton),
            ) {
                Text(stringResource(Res.string.button_retry))
            }
    }
}

@Preview
@Composable
private fun AddBookScreenPreview_Empty() {
    AddBookScreenContent(
        uiState = AddBookUiState(),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_TitleMode() {
    AddBookScreenContent(
        uiState = AddBookUiState(lookupMode = LookupMode.Title),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_Loading() {
    AddBookScreenContent(
        uiState = AddBookUiState(isbn = "9780140449136", isLoading = true),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_Error() {
    AddBookScreenContent(
        uiState = AddBookUiState(isbn = "9780140449136", error = AddBookScreenError.NotFound),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_FoundBook() {
    AddBookScreenContent(
        uiState =
            AddBookUiState(
                isbn = "9780140449136",
                foundBook =
                    BookLookupData(
                        isbn = "9780140449136",
                        title = "The Iliad",
                        authors = listOf("Homer"),
                        coverImageUrl = null,
                    ),
            ),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
    )
}
