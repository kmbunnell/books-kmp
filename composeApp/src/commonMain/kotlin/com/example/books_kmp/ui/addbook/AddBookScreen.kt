package com.example.books_kmp.ui.addbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.adaptive_add_book_placeholder
import bookskmp.composeapp.generated.resources.button_add
import bookskmp.composeapp.generated.resources.button_add_and_tag
import bookskmp.composeapp.generated.resources.button_enter_manually
import bookskmp.composeapp.generated.resources.button_isbn_lookup
import bookskmp.composeapp.generated.resources.button_look_up
import bookskmp.composeapp.generated.resources.button_retry
import bookskmp.composeapp.generated.resources.button_sign_in
import bookskmp.composeapp.generated.resources.button_title_lookup
import bookskmp.composeapp.generated.resources.camera_permission_permanently_denied
import bookskmp.composeapp.generated.resources.cd_book_cover
import bookskmp.composeapp.generated.resources.cd_close
import bookskmp.composeapp.generated.resources.cd_close_scanner
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.cd_scan_barcode
import bookskmp.composeapp.generated.resources.error_book_not_found
import bookskmp.composeapp.generated.resources.error_isbn_format
import bookskmp.composeapp.generated.resources.error_network_generic
import bookskmp.composeapp.generated.resources.error_rate_limited
import bookskmp.composeapp.generated.resources.error_scan_failed
import bookskmp.composeapp.generated.resources.error_unauthenticated
import bookskmp.composeapp.generated.resources.error_unavailable_hardware
import bookskmp.composeapp.generated.resources.info_not_found_catalog
import bookskmp.composeapp.generated.resources.label_isbn
import bookskmp.composeapp.generated.resources.label_isbn_search
import bookskmp.composeapp.generated.resources.label_title_search
import bookskmp.composeapp.generated.resources.snackbar_book_added
import bookskmp.composeapp.generated.resources.title_add_book
import bookskmp.composeapp.generated.resources.title_results_enter_manually
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.BookLookupData
import com.example.books_kmp.ui.BookCoverImage
import com.example.books_kmp.ui.DuplicateBookDialog
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import com.example.books_kmp.ui.components.InlineErrorText
import com.example.books_kmp.ui.scan.BarcodeScannerView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddBookScreen(
    onNavigateUp: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit,
    isWideScreen: Boolean = false,
) {
    val viewModel: AddBookViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddBookScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onNavigateToManualEntry = onNavigateToManualEntry,
        onNavigateToSignIn = onNavigateToSignIn,
        onNavigateToBookDetail = onNavigateToBookDetail,
        isWideScreen = isWideScreen,
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
    onNavigateToSignIn: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit,
    isWideScreen: Boolean = false,
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
                is AddBookEffect.NavigateToBookDetail -> onNavigateToBookDetail(effect.bookId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.showDuplicateDialog) {
            DuplicateBookDialog(
                onAddAnyway = { onIntent(AddBookIntent.AddAnyway) },
                onDismiss = { onIntent(AddBookIntent.DismissDuplicateDialog) },
                dialogTag = TestTags.AddBook.DuplicateDialog,
                addAnywayTag = TestTags.AddBook.DuplicateDialogAddAnywayButton,
                cancelTag = TestTags.AddBook.DuplicateDialogCancelButton,
            )
        }

        Scaffold(
            snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
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
            if (isWideScreen) {
                WideScreenLayout(
                    uiState = uiState,
                    focusRequester = focusRequester,
                    onIntent = onIntent,
                    onNavigateToSignIn = onNavigateToSignIn,
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                )
            } else {
                NarrowScreenLayout(
                    uiState = uiState,
                    focusRequester = focusRequester,
                    onIntent = onIntent,
                    onNavigateToSignIn = onNavigateToSignIn,
                    modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(TestTags.AddBook.LoadingIndicator),
                )
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
private fun WideScreenLayout(
    uiState: AddBookUiState,
    focusRequester: FocusRequester,
    onIntent: (AddBookIntent) -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            LookupSearchPanel(uiState = uiState, focusRequester = focusRequester, onIntent = onIntent)
        }

        VerticalDivider()

        val hasResultPaneContent =
            uiState.titleResults.isNotEmpty() ||
                uiState.error != null ||
                uiState.foundBook != null ||
                uiState.isLoading
        if (!hasResultPaneContent) {
            Box(
                modifier = Modifier.weight(0.62f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.adaptive_add_book_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TestTags.AddBook.WideScreenPlaceholder),
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                resultsItems(
                    uiState = uiState,
                    showSearchResults = uiState.foundBook == null,
                    onIntent = onIntent,
                    onNavigateToSignIn = onNavigateToSignIn
                )
            }
        }
    }
}

@Composable
private fun NarrowScreenLayout(
    uiState: AddBookUiState,
    focusRequester: FocusRequester,
    onIntent: (AddBookIntent) -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        if (uiState.foundBook == null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LookupSearchPanel(uiState = uiState, focusRequester = focusRequester, onIntent = onIntent)
            }
        }
        resultsItems(
            uiState = uiState,
            showSearchResults = uiState.foundBook == null,
            onIntent = onIntent,
            onNavigateToSignIn = onNavigateToSignIn,
        )
    }
}

@Composable
private fun LookupSearchPanel(
    uiState: AddBookUiState,
    focusRequester: FocusRequester,
    onIntent: (AddBookIntent) -> Unit,
) {
    val isbnLabel = stringResource(Res.string.label_isbn_search)
    val titleLabel = stringResource(Res.string.label_title_search)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.AddBook.LookupModeToggle),
    ) {
        LookupMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = uiState.lookupMode == mode,
                onClick = { onIntent(AddBookIntent.SetLookupMode(mode)) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = LookupMode.entries.size),
                label = { Text(if (mode == LookupMode.ISBN) isbnLabel else titleLabel) },
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
        isbnFormatError = uiState.isbnFormatError,
        focusRequester = focusRequester,
        onIntent = onIntent,
    )
}

private fun LazyListScope.resultsItems(
    uiState: AddBookUiState,
    showSearchResults: Boolean,
    onIntent: (AddBookIntent) -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    if (showSearchResults) {
        itemsIndexed(uiState.titleResults) { index, book ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                onClick = { onIntent(AddBookIntent.SelectTitleResult(book)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.AddBook.titleResultItem(index)),
            ) {
                BookMetadataCard(book = book, modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
        }
        if (uiState.titleResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.title_results_enter_manually),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                LookUpOptions(
                    firstButtonText = stringResource(Res.string.button_isbn_lookup),
                    onFirstButtonClick = { onIntent(AddBookIntent.SetLookupMode(LookupMode.ISBN)) },
                    onEnterManually = { onIntent(AddBookIntent.EnterManually) },
                )
            }
        }
    }
    uiState.error?.let { error ->
        item {
            ErrorSection(error = error, onIntent = onIntent, onNavigateToSignIn = onNavigateToSignIn)
        }
    }
    uiState.foundBook?.let { book ->
        item {
            Spacer(modifier = Modifier.height(16.dp))
            FoundBookCard(book = book, onIntent = onIntent)
        }
    }
}

@Composable
private fun LookupModeSection(
    lookupMode: LookupMode,
    isbn: String,
    titleQuery: String,
    isLoading: Boolean,
    isbnFormatError: Boolean,
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
            isError = isbnFormatError,
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

        if (isbnFormatError) {
            InlineErrorText(
                message = stringResource(Res.string.error_isbn_format),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { onIntent(AddBookIntent.LookupIsbn(isbn)) },
                enabled = !isLoading && isbn.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.6f).testTag(TestTags.AddBook.LookUpButton),
            ) {
                Text(stringResource(Res.string.button_look_up))
            }
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

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { onIntent(AddBookIntent.LookupByTitle(titleQuery)) },
                enabled = !isLoading && titleQuery.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.6f).testTag(TestTags.AddBook.LookUpButton),
            ) {
                Text(stringResource(Res.string.button_look_up))
            }
        }
    }
}

@Composable
private fun LookUpOptions(
    firstButtonText: String,
    onFirstButtonClick: () -> Unit,
    onEnterManually: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onFirstButtonClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(firstButtonText)
        }
        TextButton(
            onClick = onEnterManually,
            modifier = Modifier.weight(1f).testTag(TestTags.AddBook.EnterManuallyButton),
        ) {
            Text(stringResource(Res.string.button_enter_manually))
        }
    }
}

@Composable
private fun ErrorSection(
    error: AddBookScreenError,
    onIntent: (AddBookIntent) -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    val message =
        stringResource(
            when (error) {
                AddBookScreenError.NotFound -> Res.string.error_book_not_found
                AddBookScreenError.Unauthenticated -> Res.string.error_unauthenticated
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
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .testTag(TestTags.AddBook.ErrorBanner)
                .fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    when (error) {
        AddBookScreenError.NotFound -> {
            LookUpOptions(
                firstButtonText = stringResource(Res.string.button_title_lookup),
                onFirstButtonClick = { onIntent(AddBookIntent.SetLookupMode(LookupMode.Title)) },
                onEnterManually = { onIntent(AddBookIntent.EnterManually) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.info_not_found_catalog),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        AddBookScreenError.Unauthenticated ->
            TextButton(
                onClick = onNavigateToSignIn,
                modifier = Modifier.testTag(TestTags.AddBook.SignInButton),
            ) {
                Text(stringResource(Res.string.button_sign_in))
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

@Composable
private fun FoundBookCard(
    book: BookLookupData,
    onIntent: (AddBookIntent) -> Unit,
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                BookMetadataCard(
                    book = book,
                    modifier = Modifier.fillMaxWidth(),
                    coverContentDescription = stringResource(Res.string.cd_book_cover),
                    titleModifier = Modifier.testTag(TestTags.AddBook.BookPreviewTitle),
                    authorsModifier = Modifier.testTag(TestTags.AddBook.BookPreviewAuthors),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onIntent(AddBookIntent.ConfirmBook) },
                    modifier = Modifier.fillMaxWidth(0.6f).testTag(TestTags.AddBook.AddButton),
                ) {
                    Text(stringResource(Res.string.button_add))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onIntent(AddBookIntent.AddAndTag) },
                    modifier = Modifier.fillMaxWidth(0.6f).testTag(TestTags.AddBook.AddAndTagButton),
                ) {
                    Text(stringResource(Res.string.button_add_and_tag))
                }
            }
            IconButton(
                onClick = { onIntent(AddBookIntent.CancelBookPreview) },
                modifier = Modifier.align(Alignment.TopEnd).testTag(TestTags.AddBook.CancelButton),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_close),
                )
            }
        }
    }
}

@Composable
private fun BookMetadataCard(
    book: BookLookupData,
    modifier: Modifier = Modifier,
    coverContentDescription: String = "",
    titleModifier: Modifier = Modifier,
    authorsModifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        BookCoverImage(
            url = book.coverImageUrl,
            contentDescription = coverContentDescription,
            modifier = Modifier.height(200.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = titleModifier,
        )
        Text(
            text = book.authors.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = authorsModifier,
        )
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_WideEmpty() {
    AddBookScreenContent(
        uiState = AddBookUiState(),
        effects = MutableSharedFlow(),
        onIntent = {},
        onNavigateUp = {},
        onNavigateToManualEntry = {},
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
        isWideScreen = true,
    )
}

@Preview
@Composable
private fun AddBookScreenPreview_WideFoundBook() {
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
        onNavigateToSignIn = {},
        onNavigateToBookDetail = {},
        isWideScreen = true,
    )
}
