package com.example.books_kmp.util

expect fun decomposeCanonical(str: String): String

private val NONSPACING_MARKS = Regex("\\p{Mn}")
private val PUNCTUATION = Regex("\\p{P}")

fun normalise(str: String): String =
    decomposeCanonical(str)
        .replace(NONSPACING_MARKS, "")
        .replace(PUNCTUATION, "")
