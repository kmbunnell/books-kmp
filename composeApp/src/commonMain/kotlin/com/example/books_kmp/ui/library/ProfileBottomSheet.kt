package com.example.books_kmp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.label_tier_free
import bookskmp.composeapp.generated.resources.label_tier_premium
import bookskmp.composeapp.generated.resources.profile_sheet_manage_subscription
import bookskmp.composeapp.generated.resources.profile_sheet_sign_out
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    isPremium: Boolean,
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
        Text(
            text =
                if (isPremium) {
                    stringResource(
                        Res.string.label_tier_premium
                    )
                } else {
                    stringResource(Res.string.label_tier_free)
                },
            style = MaterialTheme.typography.titleMedium,
            color = if (isPremium) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(TestTags.Library.TierLabel),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(Res.string.profile_sheet_manage_subscription)) },
            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
            modifier =
                Modifier
                    .clickable {
                        onDismiss()
                        onNavigateToPaywall()
                    }
                    .testTag(TestTags.Library.ProfileSheetManageSubscription),
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.profile_sheet_sign_out)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            modifier =
                Modifier
                    .clickable {
                        onSignOut()
                        onDismiss()
                    }
                    .testTag(TestTags.Library.ProfileSheetSignOut),
        )
    }
}
