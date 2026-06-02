package com.example.books_kmp.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagError
import com.example.books_kmp.domain.tags.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TagFormMode {
    data object Create : TagFormMode

    data class Edit(val tag: Tag) : TagFormMode
}

data class TagFormState(
    val mode: TagFormMode,
    val draftName: String = "",
    val nameError: TagManagementError? = null,
)

data class TagManagementUiState(
    val defaultTags: List<Tag> = emptyList(),
    val customTags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val error: TagManagementError? = null,
    val tagFormState: TagFormState? = null,
    val pendingDeleteTag: Tag? = null,
    val pendingDeleteBookCount: Int? = null,
)

sealed interface TagManagementError {
    data object EmptyName : TagManagementError

    data object DuplicateName : TagManagementError

    data object NetworkError : TagManagementError
}

sealed interface TagManagementIntent {
    data object OpenCreateForm : TagManagementIntent

    data class OpenEditForm(val tag: Tag) : TagManagementIntent

    data class UpdateFormName(val name: String) : TagManagementIntent

    data object SubmitForm : TagManagementIntent

    data object DismissForm : TagManagementIntent

    data class RequestDeleteTag(val tag: Tag) : TagManagementIntent

    data object ConfirmDeleteTag : TagManagementIntent

    data object CancelDelete : TagManagementIntent

    data object DismissError : TagManagementIntent
}

class TagManagementViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState: StateFlow<TagManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.tagsFlow.filterNotNull().collect { tags ->
                _uiState.update {
                    it.copy(
                        defaultTags = tags.filter { tag -> tag.isDefault },
                        customTags = tags.filterNot { tag -> tag.isDefault },
                    )
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (tagRepository.getTags()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = TagManagementError.NetworkError) }
            }
        }
    }

    fun onIntent(intent: TagManagementIntent) {
        when (intent) {
            TagManagementIntent.OpenCreateForm ->
                _uiState.update { it.copy(tagFormState = TagFormState(mode = TagFormMode.Create)) }

            is TagManagementIntent.OpenEditForm -> {
                if (intent.tag.isDefault) return
                _uiState.update {
                    it.copy(
                        tagFormState =
                            TagFormState(
                                mode = TagFormMode.Edit(intent.tag),
                                draftName = intent.tag.name,
                            ),
                    )
                }
            }

            is TagManagementIntent.UpdateFormName ->
                _uiState.update { state ->
                    state.copy(
                        tagFormState =
                            state.tagFormState?.copy(
                                draftName = intent.name,
                                nameError = null,
                            ),
                    )
                }

            TagManagementIntent.DismissForm ->
                _uiState.update { it.copy(tagFormState = null) }

            TagManagementIntent.CancelDelete ->
                _uiState.update { it.copy(pendingDeleteTag = null, pendingDeleteBookCount = null) }

            TagManagementIntent.DismissError ->
                _uiState.update { it.copy(error = null) }

            TagManagementIntent.SubmitForm -> viewModelScope.launch { handleSubmitForm() }

            is TagManagementIntent.RequestDeleteTag -> viewModelScope.launch { handleRequestDelete(intent.tag) }

            TagManagementIntent.ConfirmDeleteTag -> viewModelScope.launch { handleConfirmDelete() }
        }
    }

    private suspend fun handleSubmitForm() {
        if (_uiState.value.isLoading) return
        val form = _uiState.value.tagFormState ?: return
        if (form.draftName.isBlank()) {
            _uiState.update { it.copy(tagFormState = form.copy(nameError = TagManagementError.EmptyName)) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        val result =
            when (val mode = form.mode) {
                is TagFormMode.Create -> tagRepository.createTag(form.draftName.trim())
                is TagFormMode.Edit -> tagRepository.renameTag(mode.tag.id, form.draftName.trim())
            }
        when (result) {
            is Result.Success -> _uiState.update { it.copy(tagFormState = null, isLoading = false) }
            is Result.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                when (result.error) {
                    TagError.DuplicateName ->
                        _uiState.update { state ->
                            state.copy(
                                tagFormState = state.tagFormState?.copy(nameError = TagManagementError.DuplicateName)
                            )
                        }
                    else -> _uiState.update { it.copy(error = TagManagementError.NetworkError) }
                }
            }
        }
    }

    private suspend fun handleRequestDelete(tag: Tag) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        when (val result = tagRepository.getBookCountForTag(tag.id)) {
            is Result.Success ->
                _uiState.update {
                    it.copy(isLoading = false, pendingDeleteTag = tag, pendingDeleteBookCount = result.data)
                }
            is Result.Failure ->
                _uiState.update { it.copy(isLoading = false, error = TagManagementError.NetworkError) }
        }
    }

    private suspend fun handleConfirmDelete() {
        val tag = _uiState.value.pendingDeleteTag ?: return
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        when (val result = tagRepository.deleteTag(tag.id)) {
            is Result.Success ->
                _uiState.update {
                    it.copy(pendingDeleteTag = null, pendingDeleteBookCount = null, isLoading = false)
                }
            is Result.Failure ->
                _uiState.update { it.copy(isLoading = false, error = TagManagementError.NetworkError) }
        }
    }
}
