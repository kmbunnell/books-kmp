package com.example.books_kmp.data.profile

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Profile
import com.example.books_kmp.domain.profile.ProfileRepository
import com.example.books_kmp.domain.profile.ProfileRepositoryError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

class SupabaseProfileRepository(private val supabase: SupabaseClient) : ProfileRepository {
    override suspend fun getProfile(): Result<Profile, ProfileRepositoryError> {
        val userId =
            supabase.auth.currentUserOrNull()?.id
                ?: return Result.Failure(ProfileRepositoryError.NotAuthenticated)
        return try {
            val dto =
                supabase
                    .from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                    ?: return Result.Failure(ProfileRepositoryError.NotFound)
            Result.Success(dto.toProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(ProfileRepositoryError.NetworkError)
        }
    }
}
