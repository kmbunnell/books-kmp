package com.example.books_kmp.domain

sealed interface Result<out E> {
    data object Success : Result<Nothing>

    data class Failure<out E>(val error: E) : Result<E>
}
