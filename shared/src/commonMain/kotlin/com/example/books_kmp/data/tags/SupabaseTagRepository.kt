package com.example.books_kmp.data.tags

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.TAG_SORT_ORDER
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagError
import com.example.books_kmp.domain.tags.TagRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.CancellationException

class SupabaseTagRepository(private val supabase: SupabaseClient) : TagRepository {
    internal var cache: List<Tag>? = null

    override suspend fun getTags(): Result<List<Tag>, TagError> {
        val tags =
            cache ?: when (val fetchResult = fetchTags()) {
                is Result.Failure -> return fetchResult
                is Result.Success -> fetchResult.data.also { cache = it }
            }
        return Result.Success(tags.sortedWith(TAG_SORT_ORDER))
    }

    override suspend fun createTag(name: String): Result<Tag, TagError> {
        val dupResult = isDuplicate(name, excludeId = "")
        if (dupResult is Result.Failure) return dupResult
        if ((dupResult as Result.Success).data) return Result.Failure(TagError.DuplicateName)
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        return try {
            val dto =
                supabase.from(TABLE_TAGS).insert(TagDto(name = name.trim(), userId = userId)) {
                    select()
                }.decodeSingle<TagDto>()
            val tag = dto.toTag()
            cache = cache?.plus(tag)
            Result.Success(tag)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }
    }

    override suspend fun renameTag(
        id: String,
        newName: String,
    ): Result<Tag, TagError> {
        val dupResult = isDuplicate(newName, excludeId = id)
        if (dupResult is Result.Failure) return dupResult
        if ((dupResult as Result.Success).data) return Result.Failure(TagError.DuplicateName)
        return try {
            val dto =
                supabase
                    .from(TABLE_TAGS)
                    .update({ set("name", newName.trim()) }) {
                        filter { eq("id", id) }
                        select()
                    }.decodeSingle<TagDto>()
            val tag = dto.toTag()
            cache = cache?.map { if (it.id == id) tag else it }
            Result.Success(tag)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoSuchElementException) {
            Result.Failure(TagError.NotFound)
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }
    }

    override suspend fun deleteTag(id: String): Result<Unit, TagError> =
        try {
            supabase.from(TABLE_TAGS).delete { filter { eq("id", id) } }
            cache = cache?.filterNot { it.id == id }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    override suspend fun addTagToBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError> =
        try {
            supabase.from(TABLE_BOOK_TAGS).insert(BookTagDto(bookId = bookId, tagId = tagId))
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    override suspend fun removeTagFromBook(
        bookId: String,
        tagId: String,
    ): Result<Unit, TagError> =
        try {
            supabase.from(TABLE_BOOK_TAGS).delete {
                filter {
                    eq("book_id", bookId)
                    eq("tag_id", tagId)
                }
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    override suspend fun getBookCountForTag(tagId: String): Result<Int, TagError> =
        try {
            val count =
                supabase
                    .from(TABLE_BOOK_TAGS)
                    .select {
                        head = true
                        count(Count.EXACT)
                        filter { eq("tag_id", tagId) }
                    }.countOrNull()
                    ?.toInt() ?: 0
            Result.Success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    override suspend fun getTagsForBook(bookId: String): Result<List<Tag>, TagError> =
        try {
            val tagIds =
                supabase
                    .from(TABLE_BOOK_TAGS)
                    .select { filter { eq("book_id", bookId) } }
                    .decodeList<BookTagDto>()
                    .map { it.tagId }
                    .toSet()
            when (val tagsResult = getTags()) {
                is Result.Failure -> tagsResult
                is Result.Success -> Result.Success(tagsResult.data.filter { it.id in tagIds })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    private suspend fun fetchTags(): Result<List<Tag>, TagError> =
        try {
            Result.Success(supabase.from(TABLE_TAGS).select().decodeList<TagDto>().map { it.toTag() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(TagError.NetworkError(e))
        }

    companion object {
        private const val TABLE_TAGS = "tags"
        private const val TABLE_BOOK_TAGS = "book_tags"
    }

    private suspend fun isDuplicate(
        name: String,
        excludeId: String,
    ): Result<Boolean, TagError> {
        val tagsResult = getTags()
        if (tagsResult is Result.Failure) return tagsResult
        val trimmedLower = name.trim().lowercase()
        return Result.Success(
            (tagsResult as Result.Success).data.any {
                it.name.trim().lowercase() == trimmedLower && it.id != excludeId
            },
        )
    }
}
