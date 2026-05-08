@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.books_kmp.util

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

actual fun normalise(str: String): String =
    Regex("[\\u0300-\\u036f]").replace((str as NSString).decomposedStringWithCanonicalMapping, "")
        .replace(Regex("\\p{P}"), "")
