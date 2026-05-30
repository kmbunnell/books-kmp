package com.example.books_kmp.ui.manualentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_save
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.error_author_required
import bookskmp.composeapp.generated.resources.error_save_failed
import bookskmp.composeapp.generated.resources.error_title_required
import bookskmp.composeapp.generated.resources.label_author
import bookskmp.composeapp.generated.resources.label_title
import bookskmp.composeapp.generated.resources.title_manual_entry
import com.example.books_kmp.ui.DuplicateBookDialog
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ManualEntryScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: ManualEntryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManualEntryScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreenContent(
    uiState: ManualEntryUiState,
    effects: SharedFlow<ManualEntryEffect>,
    onIntent: (ManualEntryIntent) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }

    val errorTitleRequired = stringResource(Res.string.error_title_required)
    val errorAuthorRequired = stringResource(Res.string.error_author_required)
    val errorSaveFailed = stringResource(Res.string.error_save_failed)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                ManualEntryEffect.NavigateToLibrary -> onNavigateToLibrary()
                ManualEntryEffect.NavigateBack -> onNavigateBack()
                is ManualEntryEffect.ShowError -> {
                    val message = when (effect.error) {
                        ManualEntryError.SaveFailed -> errorSaveFailed
                        ManualEntryError.TitleRequired -> errorTitleRequired
                        ManualEntryError.AuthorRequired -> errorAuthorRequired
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    if (uiState.showDuplicateDialog) {
        DuplicateBookDialog(
            onAddAnyway = { onIntent(ManualEntryIntent.AddAnyway) },
            onDismiss = { onIntent(ManualEntryIntent.DismissDuplicateDialog) },
            dialogTag = TestTags.ManualEntry.DuplicateDialog,
            addAnywayTag = TestTags.ManualEntry.DuplicateDialogAddAnywayButton,
            cancelTag = TestTags.ManualEntry.DuplicateDialogCancelButton,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_manual_entry)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ManualEntryIntent.Cancel) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.label_title)) },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(errorTitleRequired) } },
                enabled = !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.ManualEntry.TitleField),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(Res.string.label_author)) },
                singleLine = true,
                isError = uiState.authorError != null,
                supportingText = uiState.authorError?.let { { Text(errorAuthorRequired) } },
                enabled = !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.ManualEntry.AuthorField),
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag(TestTags.ManualEntry.LoadingIndicator))
                Spacer(modifier = Modifier.height(16.dp))
            }
            Button(
                onClick = { onIntent(ManualEntryIntent.SaveBook(title, author)) },
                enabled = !uiState.isLoading && title.isNotBlank() && author.isNotBlank(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.ManualEntry.SaveButton),
            ) {
                Text(stringResource(Res.string.button_save))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { onIntent(ManualEntryIntent.Cancel) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.ManualEntry.CancelButton),
            ) {
                Text(stringResource(Res.string.button_cancel))
            }
        }
    }
}
