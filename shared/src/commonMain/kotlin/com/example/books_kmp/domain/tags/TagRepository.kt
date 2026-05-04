package com.example.books_kmp.domain.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag

interface TagRepository {
    suspend fun getTags(): Result<List<Tag>, TagError>

    suspend fun createTag(name: String): Result<Tag, TagError>

    suspend fun renameTag(
        id: String,
        newName: String,
    ): Result<Tag, TagError>

    suspend fun deleteTag(id: String): Result<Unit, TagError>

    suspend fun addTagToBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError>

    suspend fun removeTagFromBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError>

    suspend fun getBookCountForTag(tagId: String): Result<Int, TagError>
}
