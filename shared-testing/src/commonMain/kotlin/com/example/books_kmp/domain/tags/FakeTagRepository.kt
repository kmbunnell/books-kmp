package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.TAG_SORT_ORDER
import com.example.books_kmp.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeTagRepository : TagRepository {
    private val tags = mutableListOf<Tag>()
    private val bookTags = mutableListOf<Pair<String, String>>()

    private val _tagsFlow = MutableStateFlow<List<Tag>?>(null)
    override val tagsFlow: StateFlow<List<Tag>?> = _tagsFlow.asStateFlow()

    var getTagsCalled = 0
    var createTagCalled = 0
    var renameTagCalled = 0
    var deleteTagCalled = 0
    var addTagToBookCalled = 0
    var removeTagFromBookCalled = 0

    fun seedTags(vararg initial: Tag) {
        tags.clear()
        tags.addAll(initial)
    }

    override suspend fun getTags(): Result<List<Tag>, TagError> {
        getTagsCalled++
        val result = tags.sortedWith(TAG_SORT_ORDER)
        _tagsFlow.value = result
        return Result.Success(result)
    }

    override suspend fun createTag(name: String): Result<Tag, TagError> {
        createTagCalled++
        val trimmed = name.trim()
        val lowerTrimmed = trimmed.lowercase()
        if (tags.any { it.name.trim().lowercase() == lowerTrimmed }) {
            return Result.Failure(TagError.DuplicateName)
        }
        val tag = Tag(id = "fake-${tags.size}", name = trimmed, isDefault = false)
        tags.add(tag)
        _tagsFlow.update { it?.plus(tag)?.sortedWith(TAG_SORT_ORDER) }
        return Result.Success(tag)
    }

    override suspend fun renameTag(
        id: String,
        newName: String,
    ): Result<Tag, TagError> {
        renameTagCalled++
        val trimmed = newName.trim()
        val lowerTrimmed = trimmed.lowercase()
        if (tags.any { it.name.trim().lowercase() == lowerTrimmed && it.id != id }) {
            return Result.Failure(TagError.DuplicateName)
        }
        val index = tags.indexOfFirst { it.id == id }
        if (index == -1) return Result.Failure(TagError.NotFound)
        val updated = tags[index].copy(name = trimmed)
        tags[index] = updated
        _tagsFlow.update { current -> current?.map { if (it.id == id) updated else it }?.sortedWith(TAG_SORT_ORDER) }
        return Result.Success(updated)
    }

    override suspend fun deleteTag(id: String): Result<Unit, TagError> {
        deleteTagCalled++
        tags.removeAll { it.id == id }
        bookTags.removeAll { it.second == id }
        _tagsFlow.update { current -> current?.filterNot { it.id == id } }
        return Result.Success(Unit)
    }

    override suspend fun addTagToBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError> {
        addTagToBookCalled++
        bookTags.add(bookId to tagId)
        return Result.Success(Unit)
    }

    override suspend fun removeTagFromBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError> {
        removeTagFromBookCalled++
        bookTags.removeAll { it.first == bookId && it.second == tagId }
        return Result.Success(Unit)
    }

    override suspend fun getBookCountForTag(tagId: String): Result<Int, TagError> =
        Result.Success(bookTags.count { it.second == tagId })
}
