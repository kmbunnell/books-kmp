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
        const val LoadingIndicator = "library_loading"
        const val SignOutButton = "library_sign_out_button"
        const val AddBookFab = "library_add_book_fab"
        const val ManageTagsButton = "library_manage_tags_button"
        const val SearchBar = "library_search_bar"
        const val SortButton = "library_sort_button"
        const val SortMenuTitleAsc = "library_sort_title_asc"
        const val SortMenuAuthorAsc = "library_sort_author_asc"
        const val BookGrid = "library_book_grid"
        const val FilterButton = "library_filter_button"
        const val FilterBadge = "library_filter_badge"
        const val FilterSheet = "library_filter_sheet"
        const val FilterSheetClearAll = "library_filter_sheet_clear_all"
        const val FilterSheetApply = "library_filter_sheet_apply"
        const val FilterSheetNoTags = "library_filter_sheet_no_tags"
        const val LibraryError = "library_error_message"
        const val RetryButton = "library_retry_button"
        const val ReloadErrorBanner = "library_reload_error_banner"
        const val ReloadErrorBannerRetry = "library_reload_error_banner_retry"
        const val ReloadErrorBannerDismiss = "library_reload_error_banner_dismiss"
        const val EmptyLibrary = "library_empty_state"
        const val AddFirstBookButton = "library_add_first_book_button"
        const val EmptyFilter = "library_empty_filter_state"

        fun filterSheetChip(tagId: String) = "library_filter_sheet_chip_$tagId"

        fun bookItem(bookId: String) = "library_book_item_$bookId"
    }

    object AddBook {
        const val NavigateUpButton = "add_book_navigate_up_button"
        const val IsbnField = "add_book_isbn_field"
        const val LookUpButton = "add_book_look_up_button"
        const val LoadingIndicator = "add_book_loading"
        const val BookPreviewTitle = "add_book_preview_title"
        const val BookPreviewAuthors = "add_book_preview_authors"
        const val AddButton = "add_book_add_button"
        const val AddAndTagButton = "add_book_add_and_tag_button"
        const val CancelButton = "add_book_cancel_button"
        const val DuplicateDialog = "add_book_duplicate_dialog"
        const val DuplicateDialogAddAnywayButton = "add_book_duplicate_dialog_add_anyway"
        const val DuplicateDialogCancelButton = "add_book_duplicate_dialog_cancel"
        const val ErrorBanner = "add_book_error_banner"
        const val RetryButton = "add_book_retry_button"
        const val EnterManuallyButton = "add_book_enter_manually_button"
        const val ScanButton = "add_book_scan_button"
        const val SignInButton = "add_book_sign_in_button"
        const val LookupModeToggle = "add_book_lookup_mode_toggle"
        const val IsbnModeButton = "add_book_isbn_mode_button"
        const val TitleModeButton = "add_book_title_mode_button"
        const val TitleField = "add_book_title_field"

        fun titleResultItem(index: Int) = "add_book_title_result_$index"
    }

    object ManualEntry {
        const val TitleField = "manual_entry_title_field"
        const val AuthorField = "manual_entry_author_field"
        const val SaveButton = "manual_entry_save_button"
        const val CancelButton = "manual_entry_cancel_button"
        const val LoadingIndicator = "manual_entry_loading"
        const val DuplicateDialog = "manual_entry_duplicate_dialog"
        const val DuplicateDialogAddAnywayButton = "manual_entry_duplicate_dialog_add_anyway"
        const val DuplicateDialogCancelButton = "manual_entry_duplicate_dialog_cancel"
    }

    object CameraPermission {
        const val RationaleDialog = "camera_permission_rationale_dialog"
        const val SettingsDialog = "camera_permission_settings_dialog"
        const val RetryButton = "camera_permission_retry_button"
        const val OpenSettingsButton = "camera_permission_open_settings_button"
        const val DismissButton = "camera_permission_dismiss_button"
    }

    object BookDetail {
        fun tagChip(tagId: String) = "book_detail_chip_$tagId"

        const val TagsSectionLabel = "book_detail_tags_label"
        const val ManageTagsButton = "book_detail_manage_tags_button"
        const val LoadingIndicator = "book_detail_loading"
        const val LoadFailedMessage = "book_detail_load_failed_message"
        const val RetryButton = "book_detail_retry_button"
        const val CoverImage = "book_detail_cover_image"
        const val DeleteButton = "book_detail_delete_button"
    }

    object ErrorPresentation {
        const val SnackbarHost = "error_presentation_snackbar_host"
        const val InlineErrorText = "error_presentation_inline_text"
        const val ConfirmationDialog = "error_presentation_confirmation_dialog"
        const val ConfirmationDialogConfirmButton = "error_presentation_confirmation_confirm"
        const val ConfirmationDialogDismissButton = "error_presentation_confirmation_dismiss"
    }

    object TagManagement {
        const val LoadingIndicator = "tag_management_loading"
        const val DefaultSectionHeader = "tag_management_default_header"
        const val CustomSectionHeader = "tag_management_custom_header"
        const val AddTagButton = "tag_management_add_tag"

        fun optionsButton(tagId: String) = "tag_options_$tagId"

        fun renameMenuItem(tagId: String) = "tag_rename_$tagId"

        fun deleteMenuItem(tagId: String) = "tag_delete_$tagId"

        const val FormNameField = "tag_form_name_field"
        const val FormSaveButton = "tag_form_save_button"
    }
}
