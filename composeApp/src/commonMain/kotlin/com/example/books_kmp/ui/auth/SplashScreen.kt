package com.example.books_kmp.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.title_splash
import com.example.books_kmp.ui.TestTags
import org.jetbrains.compose.resources.stringResource

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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.title_splash),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(modifier = Modifier.testTag(TestTags.Splash.LoadingIndicator))
            }
        }
    }
}
