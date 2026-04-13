package com.example.books_kmp.domain

sealed interface Result<out T, out E> {
    data class Success<out T>(val data: T) : Result<T, Nothing>

    data class Failure<out E>(val error: E) : Result<Nothing, E>
}
