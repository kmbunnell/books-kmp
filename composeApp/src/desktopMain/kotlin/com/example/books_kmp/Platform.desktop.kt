package com.example.books_kmp

class DesktopPlatform : Platform {
    override val name: String = "Desktop ${System.getProperty("os.name")}"
    override val isCameraScanSupported: Boolean = false
}

actual fun getPlatform(): Platform = DesktopPlatform()
