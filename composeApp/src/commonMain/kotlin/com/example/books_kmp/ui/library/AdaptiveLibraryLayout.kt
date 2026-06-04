package com.example.books_kmp.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.adaptive_detail_placeholder
import com.example.books_kmp.ui.bookdetail.BookDetailScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLibraryLayout(
    onNavigateToAddBook: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                LibraryScreen(
                    onNavigateToAddBook = onNavigateToAddBook,
                    onNavigateToTagManagement = onNavigateToTagManagement,
                    onNavigateToBookDetail = { bookId ->
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, bookId)
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val bookId = navigator.currentDestination?.contentKey
                if (bookId != null) {
                    key(bookId) {
                        BookDetailScreen(
                            bookId = bookId,
                            onNavigateUp = {
                                scope.launch { navigator.navigateBack() }
                            },
                            onNavigateToTagManagement = onNavigateToTagManagement,
                            isAdaptiveDetail = true,
                        )
                    }
                } else {
                    AdaptiveDetailPlaceholder()
                }
            }
        },
    )
}

@Composable
private fun AdaptiveDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.adaptive_detail_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
