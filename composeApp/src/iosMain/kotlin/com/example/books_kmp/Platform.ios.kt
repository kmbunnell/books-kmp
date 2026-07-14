package com.example.books_kmp

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isCameraScanSupported: Boolean = true
}

actual fun getPlatform(): Platform = IOSPlatform()
