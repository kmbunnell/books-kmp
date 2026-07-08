package com.example.books_kmp.util

actual fun decomposeCanonical(str: String): String = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
