package com.example.books_kmp

import android.app.Application
import com.example.books_kmp.config.SupabaseConfig
import com.example.books_kmp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BooksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BooksApplication)
            modules(
                appModule(
                    SupabaseConfig(
                        supabaseUrl = BuildConfig.SUPABASE_URL,
                        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
                    ),
                ),
            )
        }
    }
}
