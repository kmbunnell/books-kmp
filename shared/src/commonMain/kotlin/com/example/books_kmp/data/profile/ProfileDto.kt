package com.example.books_kmp.data.profile

import com.example.books_kmp.domain.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String = "",
    @SerialName("is_premium")
    val isPremium: Boolean = false,
)

fun ProfileDto.toProfile(): Profile =
    Profile(
        id = id,
        isPremium = isPremium,
    )
