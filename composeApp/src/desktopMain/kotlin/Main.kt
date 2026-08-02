import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.books_kmp.App
import com.example.books_kmp.config.GroqConfig
import com.example.books_kmp.config.SupabaseConfig
import com.example.books_kmp.data.local.database.databaseModule
import com.example.books_kmp.data.local.database.platformDriverModule
import com.example.books_kmp.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(
            appModule(
                config =
                    SupabaseConfig(
                        supabaseUrl = Config.SUPABASE_URL,
                        supabaseAnonKey = Config.SUPABASE_ANON_KEY,
                    ),
                groqConfig = GroqConfig(Config.GROQ_API_KEY),
            ),
            databaseModule,
            platformDriverModule(),
        )
    }
    val title = if (Config.IS_STAGING) "Books (staging)" else "Books"
    application {
        Window(onCloseRequest = ::exitApplication, title = title) {
            App()
        }
    }
}
