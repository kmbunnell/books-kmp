package com.example.books_kmp.ui.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_add_tag
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_delete
import bookskmp.composeapp.generated.resources.button_save
import bookskmp.composeapp.generated.resources.cd_collapse_section
import bookskmp.composeapp.generated.resources.cd_delete_tag
import bookskmp.composeapp.generated.resources.cd_edit_tag
import bookskmp.composeapp.generated.resources.cd_expand_section
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.error_tag_name_duplicate
import bookskmp.composeapp.generated.resources.error_tag_name_empty
import bookskmp.composeapp.generated.resources.error_tag_operation_failed
import bookskmp.composeapp.generated.resources.label_tag_name
import bookskmp.composeapp.generated.resources.message_delete_tag
import bookskmp.composeapp.generated.resources.section_custom_tags
import bookskmp.composeapp.generated.resources.section_default_tags
import bookskmp.composeapp.generated.resources.title_delete_tag
import bookskmp.composeapp.generated.resources.title_edit_tag
import bookskmp.composeapp.generated.resources.title_new_tag
import bookskmp.composeapp.generated.resources.title_tag_management
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.viewmodel.TagFormMode
import com.example.books_kmp.viewmodel.TagManagementError
import com.example.books_kmp.viewmodel.TagManagementIntent
import com.example.books_kmp.viewmodel.TagManagementIntent.CancelDelete
import com.example.books_kmp.viewmodel.TagManagementIntent.ConfirmDeleteTag
import com.example.books_kmp.viewmodel.TagManagementIntent.DismissForm
import com.example.books_kmp.viewmodel.TagManagementIntent.OpenCreateForm
import com.example.books_kmp.viewmodel.TagManagementIntent.OpenEditForm
import com.example.books_kmp.viewmodel.TagManagementIntent.RequestDeleteTag
import com.example.books_kmp.viewmodel.TagManagementIntent.SubmitForm
import com.example.books_kmp.viewmodel.TagManagementIntent.UpdateFormName
import com.example.books_kmp.viewmodel.TagManagementUiState
import com.example.books_kmp.viewmodel.TagManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TagManagementScreen(
    onNavigateUp: () -> Unit,
    viewModel: TagManagementViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    TagManagementScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreenContent(
    uiState: TagManagementUiState,
    onIntent: (TagManagementIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(Res.string.error_tag_operation_failed)

    val nameErrorText =
        when (uiState.tagFormState?.nameError) {
            TagManagementError.EmptyName -> stringResource(Res.string.error_tag_name_empty)
            TagManagementError.DuplicateName -> stringResource(Res.string.error_tag_name_duplicate)
            TagManagementError.NetworkError -> null
            null -> null
        }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onIntent(TagManagementIntent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_tag_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
            ) {
                var defaultExpanded by rememberSaveable { mutableStateOf(true) }
                var customExpanded by rememberSaveable { mutableStateOf(true) }

                // Default section
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { defaultExpanded = !defaultExpanded }
                            .testTag(TestTags.TagManagement.DefaultSectionHeader),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.section_default_tags))
                    IconButton(onClick = { defaultExpanded = !defaultExpanded }) {
                        val icon = if (defaultExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                        val cd = if (defaultExpanded) Res.string.cd_collapse_section else Res.string.cd_expand_section
                        Icon(imageVector = icon, contentDescription = stringResource(cd))
                    }
                }
                AnimatedVisibility(visible = defaultExpanded) {
                    Column {
                        uiState.defaultTags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }

                // Custom section
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { customExpanded = !customExpanded }
                            .testTag(TestTags.TagManagement.CustomSectionHeader),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.section_custom_tags))
                    IconButton(onClick = { customExpanded = !customExpanded }) {
                        val icon = if (customExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                        val cd = if (customExpanded) Res.string.cd_collapse_section else Res.string.cd_expand_section
                        Icon(imageVector = icon, contentDescription = stringResource(cd))
                    }
                }
                AnimatedVisibility(visible = customExpanded) {
                    Column {
                        uiState.customTags.forEach { tag ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag.name) },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { onIntent(OpenEditForm(tag)) },
                                    modifier = Modifier.testTag(TestTags.TagManagement.editButton(tag.id)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(Res.string.cd_edit_tag),
                                    )
                                }
                                IconButton(
                                    onClick = { onIntent(RequestDeleteTag(tag)) },
                                    modifier = Modifier.testTag(TestTags.TagManagement.deleteButton(tag.id)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(Res.string.cd_delete_tag),
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { onIntent(OpenCreateForm) },
                            modifier = Modifier.testTag(TestTags.TagManagement.AddTagButton),
                        ) {
                            Text(stringResource(Res.string.button_add_tag))
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .testTag(TestTags.TagManagement.LoadingIndicator),
                )
            }
        }
    }

    // Bottom sheet form
    if (uiState.tagFormState != null) {
        val formState = uiState.tagFormState
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(DismissForm) },
            sheetState = sheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text =
                        if (formState.mode is TagFormMode.Create) {
                            stringResource(Res.string.title_new_tag)
                        } else {
                            stringResource(Res.string.title_edit_tag)
                        },
                )
                OutlinedTextField(
                    value = formState.draftName,
                    onValueChange = { onIntent(UpdateFormName(it)) },
                    label = { Text(stringResource(Res.string.label_tag_name)) },
                    supportingText = nameErrorText?.let { { Text(it) } },
                    isError = nameErrorText != null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.TagManagement.FormNameField),
                )
                Button(
                    onClick = { onIntent(SubmitForm) },
                    enabled = formState.draftName.isNotBlank(),
                    modifier = Modifier.testTag(TestTags.TagManagement.FormSaveButton),
                ) {
                    Text(stringResource(Res.string.button_save))
                }
            }
        }
    }

    // Delete confirmation dialog
    val pendingTag = uiState.pendingDeleteTag
    if (pendingTag != null) {
        AlertDialog(
            onDismissRequest = { onIntent(CancelDelete) },
            title = { Text(stringResource(Res.string.title_delete_tag)) },
            text = {
                Text(
                    stringResource(
                        Res.string.message_delete_tag,
                        pendingTag.name,
                        uiState.pendingDeleteBookCount ?: 0,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(ConfirmDeleteTag) },
                    modifier = Modifier.testTag(TestTags.TagManagement.DeleteDialogConfirm),
                ) {
                    Text(stringResource(Res.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onIntent(CancelDelete) },
                    modifier = Modifier.testTag(TestTags.TagManagement.DeleteDialogCancel),
                ) {
                    Text(stringResource(Res.string.button_cancel))
                }
            },
        )
    }
}
