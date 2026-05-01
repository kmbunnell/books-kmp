package com.example.books_kmp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.book_placeholder
import bookskmp.composeapp.generated.resources.cd_add_book
import bookskmp.composeapp.generated.resources.cd_book_cover_in_grid
import bookskmp.composeapp.generated.resources.cd_manage_tags
import bookskmp.composeapp.generated.resources.cd_sign_out
import bookskmp.composeapp.generated.resources.cd_sort_books
import bookskmp.composeapp.generated.resources.hint_search_books
import bookskmp.composeapp.generated.resources.sort_author_asc
import bookskmp.composeapp.generated.resources.sort_title_asc
import bookskmp.composeapp.generated.resources.title_library
import coil3.compose.AsyncImage
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.LibraryIntent
import com.example.books_kmp.viewmodel.LibraryUiState
import com.example.books_kmp.viewmodel.LibraryViewModel
import com.example.books_kmp.viewmodel.SortOrder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onSignOut: () -> Unit,
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToBookDetail: (String) -> Unit = {},
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onIntent(LibraryIntent.Refresh)
        }
    }
    LibraryScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onSignOut = onSignOut,
        onNavigateToAddBook = onNavigateToAddBook,
        onNavigateToTagManagement = onNavigateToTagManagement,
        onNavigateToBookDetail = onNavigateToBookDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    onIntent: (LibraryIntent) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToBookDetail: (String) -> Unit = {},
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_library)) },
                actions = {
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
                        onClick = onSignOut,
                        modifier = Modifier.testTag(TestTags.Library.SignOutButton),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(Res.string.cd_sign_out),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBook,
                modifier = Modifier.testTag(TestTags.Library.AddBookFab),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.cd_add_book),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.tags, key = { it.id }) { tag ->
                    FilterChip(
                        selected = tag.id in uiState.activeFilterTagIds,
                        onClick = { onIntent(LibraryIntent.ToggleFilter(tag.id)) },
                        label = { Text(tag.name) },
                        modifier = Modifier.testTag(TestTags.Library.filterChip(tag.id)),
                    )
                }
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

@Composable
private fun BookGridItem(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(4.dp)
                .clickable(onClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = stringResource(Res.string.cd_book_cover_in_grid),
            placeholder = painterResource(Res.drawable.book_placeholder),
            error = painterResource(Res.drawable.book_placeholder),
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
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = book.authors.firstOrNull() ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
