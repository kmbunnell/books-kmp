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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_add_tag
import bookskmp.composeapp.generated.resources.button_cancel
import bookskmp.composeapp.generated.resources.button_delete
import bookskmp.composeapp.generated.resources.button_save
import bookskmp.composeapp.generated.resources.cd_collapse_section
import bookskmp.composeapp.generated.resources.cd_expand_section
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.cd_tag_options
import bookskmp.composeapp.generated.resources.error_tag_name_duplicate
import bookskmp.composeapp.generated.resources.error_tag_name_empty
import bookskmp.composeapp.generated.resources.error_tag_operation_failed
import bookskmp.composeapp.generated.resources.label_tag_name
import bookskmp.composeapp.generated.resources.menu_delete_tag
import bookskmp.composeapp.generated.resources.menu_rename_tag
import bookskmp.composeapp.generated.resources.message_delete_tag
import bookskmp.composeapp.generated.resources.section_custom_tags
import bookskmp.composeapp.generated.resources.section_default_tags
import bookskmp.composeapp.generated.resources.title_delete_tag
import bookskmp.composeapp.generated.resources.title_edit_tag
import bookskmp.composeapp.generated.resources.title_new_tag
import bookskmp.composeapp.generated.resources.title_tag_management
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import com.example.books_kmp.ui.components.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_TAG_NAME_LENGTH = 20

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    testTag: String,
    onToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = onToggle) {
                val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                val cd = if (expanded) Res.string.cd_collapse_section else Res.string.cd_expand_section
                Icon(imageVector = icon, contentDescription = stringResource(cd))
            }
        }
    }
}

@Composable
fun TagManagementScreen(
    onNavigateUp: () -> Unit,
    viewModel: TagManagementViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
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
                        .verticalScroll(rememberScrollState()),
            ) {
                var defaultExpanded by rememberSaveable { mutableStateOf(false) }
                var customExpanded by rememberSaveable { mutableStateOf(true) }

                CollapsibleSectionHeader(
                    title = stringResource(Res.string.section_default_tags),
                    expanded = defaultExpanded,
                    testTag = TestTags.TagManagement.DefaultSectionHeader,
                    onToggle = { defaultExpanded = !defaultExpanded },
                )
                AnimatedVisibility(visible = defaultExpanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        uiState.defaultTags.forEach { tag ->
                            Text(
                                text = tag.name,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                CollapsibleSectionHeader(
                    title = stringResource(Res.string.section_custom_tags),
                    expanded = customExpanded,
                    testTag = TestTags.TagManagement.CustomSectionHeader,
                    onToggle = { customExpanded = !customExpanded },
                )
                AnimatedVisibility(visible = customExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var expandedTagId by rememberSaveable { mutableStateOf<String?>(null) }
                        uiState.customTags.forEach { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = tag.name,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                                Box {
                                    IconButton(
                                        onClick = { expandedTagId = tag.id },
                                        modifier = Modifier.testTag(TestTags.TagManagement.optionsButton(tag.id)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = stringResource(Res.string.cd_tag_options),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expandedTagId == tag.id,
                                        onDismissRequest = { expandedTagId = null },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.menu_rename_tag)) },
                                            onClick = {
                                                expandedTagId = null
                                                onIntent(TagManagementIntent.OpenEditForm(tag))
                                            },
                                            modifier = Modifier.testTag(TestTags.TagManagement.renameMenuItem(tag.id)),
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.menu_delete_tag)) },
                                            onClick = {
                                                expandedTagId = null
                                                onIntent(TagManagementIntent.RequestDeleteTag(tag))
                                            },
                                            modifier = Modifier.testTag(TestTags.TagManagement.deleteMenuItem(tag.id)),
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = { onIntent(TagManagementIntent.OpenCreateForm) },
                            modifier =
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(TestTags.TagManagement.AddTagButton),
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
            onDismissRequest = { onIntent(TagManagementIntent.DismissForm) },
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
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = formState.draftName,
                    onValueChange = {
                        if (it.length <= MAX_TAG_NAME_LENGTH) {
                            onIntent(
                                TagManagementIntent.UpdateFormName(
                                    it
                                )
                            )
                        }
                    },
                    label = { Text(stringResource(Res.string.label_tag_name)) },
                    supportingText = {
                        if (nameErrorText != null) {
                            Text(nameErrorText)
                        } else {
                            Text("${formState.draftName.length}/$MAX_TAG_NAME_LENGTH")
                        }
                    },
                    isError = nameErrorText != null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.TagManagement.FormNameField),
                )
                Button(
                    onClick = { onIntent(TagManagementIntent.SubmitForm) },
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
        ConfirmationDialog(
            title = stringResource(Res.string.title_delete_tag),
            message =
                stringResource(
                    Res.string.message_delete_tag,
                    pendingTag.name,
                    uiState.pendingDeleteBookCount ?: 0,
                ),
            confirmLabel = stringResource(Res.string.button_delete),
            dismissLabel = stringResource(Res.string.button_cancel),
            onConfirm = { onIntent(TagManagementIntent.ConfirmDeleteTag) },
            onDismiss = { onIntent(TagManagementIntent.CancelDelete) },
        )
    }
}
