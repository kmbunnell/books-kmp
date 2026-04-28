package com.example.books_kmp

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Splash : Route

    @Serializable data object SignIn : Route

    @Serializable data object SignUp : Route

    @Serializable data object Library : Route

    @Serializable data object AddBook : Route

    @Serializable data object ManualEntry : Route

    @Serializable data object TagManagement : Route

    @Serializable data class BookDetail(val bookId: String) : Route
}
