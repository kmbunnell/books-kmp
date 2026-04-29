package com.example.books_kmp.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.cd_add_book
import bookskmp.composeapp.generated.resources.cd_manage_tags
import bookskmp.composeapp.generated.resources.cd_sign_out
import bookskmp.composeapp.generated.resources.title_library
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.LibraryIntent
import com.example.books_kmp.viewmodel.LibraryUiState
import com.example.books_kmp.viewmodel.LibraryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onSignOut: () -> Unit,
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_library)) },
                actions = {
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
        LazyRow(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 8.dp),
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
    }
}
