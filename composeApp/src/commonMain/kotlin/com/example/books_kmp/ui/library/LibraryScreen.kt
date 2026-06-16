package com.example.books_kmp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.action_retry
import bookskmp.composeapp.generated.resources.cd_account
import bookskmp.composeapp.generated.resources.cd_add_book
import bookskmp.composeapp.generated.resources.cd_book_cover_in_grid
import bookskmp.composeapp.generated.resources.cd_close
import bookskmp.composeapp.generated.resources.cd_filter_books
import bookskmp.composeapp.generated.resources.cd_manage_tags
import bookskmp.composeapp.generated.resources.cd_sort_books
import bookskmp.composeapp.generated.resources.error_library_limit_reached
import bookskmp.composeapp.generated.resources.error_library_load_failed
import bookskmp.composeapp.generated.resources.error_sign_out_failed
import bookskmp.composeapp.generated.resources.hint_search_books
import bookskmp.composeapp.generated.resources.library_empty_add_first
import bookskmp.composeapp.generated.resources.library_empty_filter
import bookskmp.composeapp.generated.resources.library_empty_title
import bookskmp.composeapp.generated.resources.paywall_button_go_premium
import bookskmp.composeapp.generated.resources.placeholder
import bookskmp.composeapp.generated.resources.sort_author_asc
import bookskmp.composeapp.generated.resources.sort_title_asc
import bookskmp.composeapp.generated.resources.title_library
import coil3.compose.AsyncImage
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToBookDetail: (String) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val loadFailedMessage = stringResource(Res.string.error_library_load_failed)
    val signOutFailedMessage = stringResource(Res.string.error_sign_out_failed)
    val limitReachedMessage = stringResource(Res.string.error_library_limit_reached)
    val goPremiumLabel = stringResource(Res.string.paywall_button_go_premium)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.ShowError -> {
                    when (effect.error) {
                        LibraryError.LibraryLimitReached -> {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = limitReachedMessage,
                                    actionLabel = goPremiumLabel,
                                )
                            if (result == SnackbarResult.ActionPerformed) onNavigateToPaywall()
                        }
                        LibraryError.LoadFailed -> snackbarHostState.showSnackbar(loadFailedMessage)
                        LibraryError.SignOutFailed -> snackbarHostState.showSnackbar(signOutFailedMessage)
                    }
                }
                LibraryEffect.NavigateToAddBook -> onNavigateToAddBook()
            }
        }
    }
    LibraryScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateToTagManagement = onNavigateToTagManagement,
        onNavigateToBookDetail = onNavigateToBookDetail,
        onNavigateToPaywall = onNavigateToPaywall,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    onIntent: (LibraryIntent) -> Unit,
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToBookDetail: (String) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.title_library))
                },
                actions = {
                    if (uiState.error == null) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeFilterTagIds.isNotEmpty()) {
                                    Badge(modifier = Modifier.testTag(TestTags.Library.FilterBadge)) {
                                        Text("${uiState.activeFilterTagIds.size}")
                                    }
                                }
                            },
                        ) {
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier.testTag(TestTags.Library.FilterButton),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(Res.string.cd_filter_books),
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag(TestTags.Library.SortButton),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = stringResource(Res.string.cd_sort_books),
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sort_title_asc)) },
                                onClick = {
                                    onIntent(LibraryIntent.ChangeSortOrder(SortOrder.TITLE_ASC))
                                    sortMenuExpanded = false
                                },
                                modifier = Modifier.testTag(TestTags.Library.SortMenuTitleAsc),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sort_author_asc)) },
                                onClick = {
                                    onIntent(LibraryIntent.ChangeSortOrder(SortOrder.AUTHOR_ASC))
                                    sortMenuExpanded = false
                                },
                                modifier = Modifier.testTag(TestTags.Library.SortMenuAuthorAsc),
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToTagManagement,
                        modifier = Modifier.testTag(TestTags.Library.ManageTagsButton),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalOffer,
                            contentDescription = stringResource(Res.string.cd_manage_tags),
                        )
                    }
                    IconButton(
                        onClick = { showProfileSheet = true },
                        modifier = Modifier.testTag(TestTags.Library.ProfileButton),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(Res.string.cd_account),
                            tint = if (uiState.isPremium) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(LibraryIntent.NavigateToAddBook) },
                modifier = Modifier.testTag(TestTags.Library.AddBookFab),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.cd_add_book),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.error != null && uiState.books.isEmpty() -> {
                LibraryErrorContent(
                    onRetry = { onIntent(LibraryIntent.Refresh) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            uiState.isLoading && uiState.books.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.testTag(TestTags.Library.LoadingIndicator),
                    )
                }
            }

            uiState.books.isEmpty() -> {
                EmptyLibraryContent(
                    onAddFirstBook = { onIntent(LibraryIntent.NavigateToAddBook) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            uiState.filteredBooks.isEmpty() -> {
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (uiState.error != null) {
                        ReloadErrorBanner(
                            onRetry = { onIntent(LibraryIntent.Refresh) },
                            onDismiss = { onIntent(LibraryIntent.DismissError) },
                        )
                    }
                    EmptyFilterContent(
                        searchQuery = uiState.searchQuery,
                        onIntent = onIntent,
                    )
                }
            }

            else -> {
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (uiState.error != null) {
                        ReloadErrorBanner(
                            onRetry = { onIntent(LibraryIntent.Refresh) },
                            onDismiss = { onIntent(LibraryIntent.DismissError) },
                        )
                    }
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { onIntent(LibraryIntent.ChangeSearchQuery(it)) },
                        placeholder = { Text(stringResource(Res.string.hint_search_books)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag(TestTags.Library.SearchBar),
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        modifier = Modifier.fillMaxSize().testTag(TestTags.Library.BookGrid),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.filteredBooks, key = { it.id }) { book ->
                            BookGridItem(
                                book = book,
                                onClick = { onNavigateToBookDetail(book.id) },
                                modifier = Modifier.testTag(TestTags.Library.bookItem(book.id)),
                            )
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            TagFilterBottomSheet(
                tags = uiState.tags,
                selectedTagIds = uiState.activeFilterTagIds,
                onTagSelected = { onIntent(LibraryIntent.ToggleFilter(it)) },
                onClearAll = { onIntent(LibraryIntent.ClearFilters) },
                onDismiss = { showFilterSheet = false },
            )
        }

        if (showProfileSheet) {
            ProfileBottomSheet(
                isPremium = uiState.isPremium,
                onNavigateToPaywall = onNavigateToPaywall,
                onSignOut = { onIntent(LibraryIntent.SignOut) },
                onDismiss = { showProfileSheet = false },
            )
        }
    }
}

@Composable
private fun LibraryErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.error_library_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(TestTags.Library.LibraryError),
        )
        Button(
            onClick = onRetry,
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .testTag(TestTags.Library.RetryButton),
        ) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

@Composable
private fun ReloadErrorBanner(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(TestTags.Library.ReloadErrorBanner),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.error_library_load_failed),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier.testTag(TestTags.Library.ReloadErrorBannerRetry),
            ) {
                Text(stringResource(Res.string.action_retry))
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Library.ReloadErrorBannerDismiss),
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
private fun EmptyLibraryContent(
    onAddFirstBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.library_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(TestTags.Library.EmptyLibrary),
        )
        Button(
            onClick = onAddFirstBook,
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .testTag(TestTags.Library.AddFirstBookButton),
        ) {
            Text(stringResource(Res.string.library_empty_add_first))
        }
    }
}

@Composable
private fun EmptyFilterContent(
    searchQuery: String,
    onIntent: (LibraryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onIntent(LibraryIntent.ChangeSearchQuery(it)) },
            placeholder = { Text(stringResource(Res.string.hint_search_books)) },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag(TestTags.Library.SearchBar),
        )
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.library_empty_filter),
                modifier = Modifier.testTag(TestTags.Library.EmptyFilter),
            )
        }
    }
}

@Composable
private fun BookGridItem(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clickable(onClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = stringResource(Res.string.cd_book_cover_in_grid),
            placeholder = painterResource(Res.drawable.placeholder),
            error = painterResource(Res.drawable.placeholder),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(4.dp)),
        )
        Text(
            text = book.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = book.authors.firstOrNull() ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
