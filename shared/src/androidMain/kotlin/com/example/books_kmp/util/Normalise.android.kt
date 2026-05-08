package com.example.books_kmp.util

actual fun normalise(str: String): String =
    java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}"), "")
