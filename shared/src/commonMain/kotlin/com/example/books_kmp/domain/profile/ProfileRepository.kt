package com.example.books_kmp.domain.profile

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Profile

interface ProfileRepository {
    suspend fun getProfile(): Result<Profile, ProfileRepositoryError>
}
