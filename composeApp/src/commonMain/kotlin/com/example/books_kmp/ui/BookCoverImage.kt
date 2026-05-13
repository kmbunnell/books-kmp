package com.example.books_kmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.placeholder
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun BookCoverImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onSuccess: () -> Unit = {},
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        placeholder = painterResource(Res.drawable.placeholder),
        error = painterResource(Res.drawable.placeholder),
        contentScale = ContentScale.Fit,
        onSuccess = { onSuccess() },
        modifier = modifier,
    )
}
