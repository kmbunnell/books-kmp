package com.example.books_kmp.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.books_kmp.ui.TestTags

@Composable
fun SplashScreen(
    uiState: AuthUiState,
    onAuthenticated: () -> Unit,
    onNotAuthenticated: () -> Unit,
) {
    LaunchedEffect(uiState) {
        when {
            uiState.isAuthenticated -> onAuthenticated()
            !uiState.isLoading -> onNotAuthenticated()
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag(TestTags.Splash.LoadingIndicator))
        }
    }
}
