package com.example.books_kmp.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.filter_sheet_done
import bookskmp.composeapp.generated.resources.filter_sheet_clear_all
import bookskmp.composeapp.generated.resources.filter_sheet_no_tags
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterBottomSheet(
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagSelected: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(TestTags.Library.FilterSheet),
    ) {
        if (tags.isEmpty()) {
            Text(
                text = stringResource(Res.string.filter_sheet_no_tags),
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(TestTags.Library.FilterSheetNoTags),
            )
        } else {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in selectedTagIds,
                        onClick = { onTagSelected(tag.id) },
                        label = { Text(tag.name) },
                        modifier = Modifier.testTag(TestTags.Library.filterSheetChip(tag.id)),
                    )
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = {
                    onClearAll()
                    onDismiss()
                },
                modifier = Modifier.testTag(TestTags.Library.FilterSheetClearAll),
            ) {
                Text(stringResource(Res.string.filter_sheet_clear_all))
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Library.FilterSheetApply),
            ) {
                Text(stringResource(Res.string.filter_sheet_done))
            }
        }
    }
}
