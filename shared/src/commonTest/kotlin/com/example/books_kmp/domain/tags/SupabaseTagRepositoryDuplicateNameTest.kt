package com.example.books_kmp.domain.tags

import com.example.books_kmp.data.tags.SupabaseTagRepository
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class SupabaseTagRepositoryDuplicateNameTest {
    private val supabase =
        createSupabaseClient("https://test.supabase.co", "test-key") {
            install(Postgrest)
        }
    private val repo = SupabaseTagRepository(supabase)

    private fun seedTags(vararg tags: Tag) {
        repo.cache = tags.toList()
    }

    @Test
    fun `createTag returns DuplicateName when name matches existing tag case-insensitively`() =
        runTest {
            seedTags(Tag(id = "1", name = "Fiction", isDefault = false))
            val result = repo.createTag("fiction")
            assertIs<Result.Failure<TagError>>(result)
            assertEquals(TagError.DuplicateName, result.error)
        }

    @Test
    fun `createTag returns DuplicateName for name differing only by whitespace trimming`() =
        runTest {
            seedTags(Tag(id = "1", name = "Read", isDefault = false))
            val result = repo.createTag(" Read ")
            assertIs<Result.Failure<TagError>>(result)
            assertEquals(TagError.DuplicateName, result.error)
        }

    @Test
    fun `renameTag returns DuplicateName when new name conflicts with a different tag`() =
        runTest {
            seedTags(
                Tag(id = "1", name = "Fiction", isDefault = false),
                Tag(id = "2", name = "Read", isDefault = false),
            )
            val result = repo.renameTag("1", "read")
            assertIs<Result.Failure<TagError>>(result)
            assertEquals(TagError.DuplicateName, result.error)
        }

    @Test
    fun `getTags returns default tags before custom tags, each group sorted alphabetically`() =
        runTest {
            seedTags(
                Tag(id = "1", name = "Zzz", isDefault = false),
                Tag(id = "2", name = "Fiction", isDefault = true),
                Tag(id = "3", name = "Aaa", isDefault = false),
                Tag(id = "4", name = "History", isDefault = true),
            )
            val result = repo.getTags()
            assertIs<Result.Success<List<Tag>>>(result)
            assertEquals(listOf("Fiction", "History", "Aaa", "Zzz"), result.data.map { it.name })
        }
}
