@file:Suppress("ktlint:standard:function-naming")

package com.example.books_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.books_kmp.di.appModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun initKoin(
    supabaseUrl: String,
    supabaseKey: String
) {
    startKoin { modules(appModule(supabaseUrl, supabaseKey)) }
}

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
