package com.example.books_kmp.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.cd_add_book
import bookskmp.composeapp.generated.resources.cd_manage_tags
import bookskmp.composeapp.generated.resources.cd_sign_out
import bookskmp.composeapp.generated.resources.snackbar_book_added
import bookskmp.composeapp.generated.resources.title_library
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    onSignOut: () -> Unit,
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    showBookAdded: Boolean = false,
    onBookAddedShown: () -> Unit = {},
) {
    LibraryScreenContent(
        onSignOut = onSignOut,
        onNavigateToAddBook = onNavigateToAddBook,
        onNavigateToTagManagement = onNavigateToTagManagement,
        showBookAdded = showBookAdded,
        onBookAddedShown = onBookAddedShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    onSignOut: () -> Unit,
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    showBookAdded: Boolean = false,
    onBookAddedShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarBookAdded = stringResource(Res.string.snackbar_book_added)

    LaunchedEffect(showBookAdded) {
        if (showBookAdded) {
            snackbarHostState.showSnackbar(snackbarBookAdded)
            onBookAddedShown()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        // TODO: apply innerPadding to content when Library body is implemented
    }
}
