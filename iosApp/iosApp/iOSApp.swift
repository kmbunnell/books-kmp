import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        guard
            let info = Bundle.main.infoDictionary,
            let supabaseUrl = info["SUPABASE_URL"] as? String,
            !supabaseUrl.trimmingCharacters(in: .whitespaces).isEmpty,
            let supabaseKey = info["SUPABASE_ANON_KEY"] as? String,
            !supabaseKey.trimmingCharacters(in: .whitespaces).isEmpty
        else {
            fatalError("SUPABASE_URL and SUPABASE_ANON_KEY must be set in Secrets.xcconfig")
        }
        MainViewControllerKt.doInitKoin(supabaseUrl: supabaseUrl, supabaseKey: supabaseKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}