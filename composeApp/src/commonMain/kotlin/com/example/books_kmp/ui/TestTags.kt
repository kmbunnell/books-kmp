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
        const val AddBookFab = "library_add_book_fab"
    }

    object AddBook {
        const val NavigateUpButton = "add_book_navigate_up_button"
        const val IsbnField = "add_book_isbn_field"
        const val LookUpButton = "add_book_look_up_button"
        const val LoadingIndicator = "add_book_loading"
        const val BookPreviewTitle = "add_book_preview_title"
        const val BookPreviewAuthors = "add_book_preview_authors"
        const val BookPreviewCover = "add_book_preview_cover"
        const val AddButton = "add_book_add_button"
        const val CancelButton = "add_book_cancel_button"
        const val DuplicateDialog = "add_book_duplicate_dialog"
        const val DuplicateDialogOkButton = "add_book_duplicate_dialog_ok"
        const val ErrorBanner = "add_book_error_banner"
        const val RetryButton = "add_book_retry_button"
        const val EnterManuallyButton = "add_book_enter_manually_button"
        const val ScanButton = "add_book_scan_button"
    }

    object ManualEntry {
        const val TitleField = "manual_entry_title_field"
        const val AuthorField = "manual_entry_author_field"
        const val SaveButton = "manual_entry_save_button"
        const val CancelButton = "manual_entry_cancel_button"
        const val LoadingIndicator = "manual_entry_loading"
    }

    object CameraPermission {
        const val RationaleDialog = "camera_permission_rationale_dialog"
        const val SettingsDialog = "camera_permission_settings_dialog"
        const val RetryButton = "camera_permission_retry_button"
        const val OpenSettingsButton = "camera_permission_open_settings_button"
        const val DismissButton = "camera_permission_dismiss_button"
    }
}
