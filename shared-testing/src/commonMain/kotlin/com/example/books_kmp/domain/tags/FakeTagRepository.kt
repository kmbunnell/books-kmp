package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.TAG_SORT_ORDER
import com.example.books_kmp.domain.model.Tag

class FakeTagRepository : TagRepository {
    private val tags = mutableListOf<Tag>()
    private val bookTags = mutableListOf<Pair<String, String>>()

    var getTagsCalled = 0
    var createTagCalled = 0
    var renameTagCalled = 0
    var deleteTagCalled = 0
    var addTagToBookCalled = 0
    var removeTagFromBookCalled = 0
    var getTagsForBookCalled = 0

    fun seedTags(vararg initial: Tag) {
        tags.clear()
        tags.addAll(initial)
    }

    fun seedBookTags(vararg pairs: Pair<String, String>) {
        bookTags.addAll(pairs)
    }

    override suspend fun getTags(): Result<List<Tag>, TagError> {
        getTagsCalled++
        return Result.Success(tags.sortedWith(TAG_SORT_ORDER))
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
        return Result.Success(updated)
    }

    override suspend fun deleteTag(id: String): Result<Unit, TagError> {
        deleteTagCalled++
        tags.removeAll { it.id == id }
        bookTags.removeAll { it.second == id }
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

    override suspend fun getTagsForBook(bookId: String): Result<List<Tag>, TagError> {
        getTagsForBookCalled++
        val ids = bookTags.filter { it.first == bookId }.map { it.second }.toSet()
        return Result.Success(tags.filter { it.id in ids })
    }
}
