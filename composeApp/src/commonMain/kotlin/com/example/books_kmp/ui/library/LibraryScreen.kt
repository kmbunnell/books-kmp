package com.example.books_kmp.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.cd_sign_out
import bookskmp.composeapp.generated.resources.title_library
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(onSignOut: () -> Unit) {
    LibraryScreenContent(onSignOut = onSignOut)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(onSignOut: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_library)) },
                actions = {
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
    ) { _ -> }
}
