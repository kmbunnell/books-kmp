package com.example.books_kmp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.profile_sheet_manage_subscription
import bookskmp.composeapp.generated.resources.profile_sheet_sign_out
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    onNavigateToPaywall: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(TestTags.Library.ProfileSheet),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.profile_sheet_manage_subscription)) },
            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
            modifier =
                Modifier
                    .clickable { onDismiss(); onNavigateToPaywall() }
                    .testTag(TestTags.Library.ProfileSheetManageSubscription),
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.profile_sheet_sign_out)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            modifier =
                Modifier
                    .clickable { onSignOut(); onDismiss() }
                    .testTag(TestTags.Library.ProfileSheetSignOut),
        )
    }
}
