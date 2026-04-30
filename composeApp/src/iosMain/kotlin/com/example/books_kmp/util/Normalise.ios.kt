@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.books_kmp.util

import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateMutableCopy
import platform.CoreFoundation.CFStringNormalize
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringNormalizationFormD

actual fun normalise(str: String): String {
    val cfStr =
        CFStringCreateMutableCopy(kCFAllocatorDefault, 0, str as CFStringRef)
            ?: return str
    CFStringNormalize(cfStr, kCFStringNormalizationFormD)
    val result = Regex("[\\u0300-\\u036f]").replace(cfStr as String, "")
    CFRelease(cfStr)
    return result
}
