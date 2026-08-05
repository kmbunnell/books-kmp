@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.books_kmp.util

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

// Kotlin String and NSString are toll-free bridged at the ObjC runtime level, so this cast
// succeeds in practice even though the compiler can't statically prove the relationship.
@Suppress("CAST_NEVER_SUCCEEDS")
actual fun decomposeCanonical(str: String): String = (str as NSString).decomposedStringWithCanonicalMapping
