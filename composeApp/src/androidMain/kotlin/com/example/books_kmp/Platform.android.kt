package com.example.books_kmp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val isCameraScanSupported: Boolean = true
}

actual fun getPlatform(): Platform = AndroidPlatform()
