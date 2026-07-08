@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.books_kmp.util

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

actual fun decomposeCanonical(str: String): String = (str as NSString).decomposedStringWithCanonicalMapping
