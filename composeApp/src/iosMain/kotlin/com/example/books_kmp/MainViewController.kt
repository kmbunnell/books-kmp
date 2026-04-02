@file:Suppress("ktlint:standard:function-naming")

package com.example.books_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.books_kmp.di.appModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun initKoin() {
    // TODO SHELVD-51: replace with real iOS credentials
    startKoin { modules(appModule("PLACEHOLDER_URL", "PLACEHOLDER_KEY")) }
}

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
