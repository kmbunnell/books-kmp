package com.example.books_kmp.ui

@Suppress("ktlint:standard:property-naming")
object TestTags {
    object SignIn {
        const val EmailField = "sign_in_email_field"
        const val PasswordField = "sign_in_password_field"
        const val LoadingIndicator = "sign_in_loading"
        const val SignInButton = "sign_in_button"
        const val PasswordToggle = "sign_in_password_toggle"
    }

    object SignUp {
        const val EmailField = "sign_up_email_field"
        const val PasswordField = "sign_up_password_field"
        const val ConfirmPasswordField = "sign_up_confirm_password_field"
        const val CreateAccountButton = "sign_up_create_account_button"
        const val LoadingIndicator = "sign_up_loading"
        const val PasswordToggle = "sign_up_password_toggle"
        const val ConfirmPasswordToggle = "sign_up_confirm_password_toggle"
        const val PasswordHint = "sign_up_password_hint"
    }

    object Splash {
        const val LoadingIndicator = "splash_loading"
    }

    object Library {
        const val SignOutButton = "library_sign_out_button"
    }
}
