@file:Suppress("ktlint:standard:function-naming")

package com.example.books_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.books_kmp.di.appModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin { modules(appModule) }
    return ComposeUIViewController { App() }
}
