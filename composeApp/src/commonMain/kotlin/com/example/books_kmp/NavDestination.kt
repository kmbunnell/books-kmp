package com.example.books_kmp

enum class NavDestination(val route: String) {
    Splash("splash"),
    SignIn("sign_in"),
    SignUp("sign_up"),
    Library("library"),
    AddBook("add_book"),
    ManualEntry("manual_entry"),
    TagManagement("tag_management"),
    BookDetail("book_detail/{bookId}");

    companion object {
        fun bookDetailRoute(bookId: String) = "book_detail/$bookId"
    }
}
