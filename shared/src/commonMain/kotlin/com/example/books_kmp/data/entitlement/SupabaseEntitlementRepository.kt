package com.example.books_kmp.data.entitlement

import com.example.books_kmp.data.profile.ProfileDto
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.entitlement.EntitlementError
import com.example.books_kmp.domain.entitlement.EntitlementRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

class SupabaseEntitlementRepository(
    private val supabase: SupabaseClient,
) : EntitlementRepository {
    override suspend fun fetchIsPremium(userId: String): Result<Boolean, EntitlementError> =
        try {
            val dto =
                supabase
                    .from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
            Result.Success(dto?.isPremium ?: false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(EntitlementError.NetworkError)
        }

    override suspend fun setPremiumStatus(
        userId: String,
        premium: Boolean
    ): Result<Unit, EntitlementError> =
        try {
            supabase.from("profiles").update({ set("is_premium", premium) }) { filter { eq("id", userId) } }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(EntitlementError.NetworkError)
        }
}
