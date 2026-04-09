package com.example.books_kmp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_sign_in
import bookskmp.composeapp.generated.resources.error_email_required
import bookskmp.composeapp.generated.resources.error_invalid_credentials
import bookskmp.composeapp.generated.resources.error_sign_in_failed
import bookskmp.composeapp.generated.resources.error_password_required
import bookskmp.composeapp.generated.resources.label_email
import bookskmp.composeapp.generated.resources.label_password
import bookskmp.composeapp.generated.resources.sign_in_sign_up_prompt
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.auth.SignInError
import com.example.books_kmp.viewmodel.SignInEffect
import com.example.books_kmp.viewmodel.SignInIntent
import com.example.books_kmp.viewmodel.SignInUiState
import com.example.books_kmp.viewmodel.SignInViewModel
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(onNavigateToSignUp: () -> Unit) {
    val viewModel: SignInViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    SignInScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onSignIn = { email, password ->
            viewModel.onIntent(SignInIntent.SignIn(email, password))
        },
        onNavigateToSignUp = onNavigateToSignUp,
    )
}

@Composable
fun SignInScreenContent(
    uiState: SignInUiState,
    effects: SharedFlow<SignInEffect>,
    onSignIn: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val errorEmailRequired = stringResource(Res.string.error_email_required)
    val errorPasswordRequired = stringResource(Res.string.error_password_required)
    val errorInvalidCredentials = stringResource(Res.string.error_invalid_credentials)
    val errorSignInFailed = stringResource(Res.string.error_sign_in_failed)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is SignInEffect.ShowError ->
                    snackbarHostState.showSnackbar(
                        when (effect.error) {
                            SignInError.InvalidCredentials -> errorInvalidCredentials
                            SignInError.EmptyEmail,
                            SignInError.EmptyPassword,
                            SignInError.SignInFailed -> errorSignInFailed
                        },
                    )
            }
        }
    }

    AuthFormLayout(snackbarHostState = snackbarHostState) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(Res.string.label_email)) },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { { Text(errorEmailRequired) } },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignIn.EmailField),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = { onSignIn(email, password) },
                ),
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { { Text(errorPasswordRequired) } },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignIn.PasswordField),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.testTag(TestTags.SignIn.LoadingIndicator))
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = { onSignIn(email, password) },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignIn.SignInButton),
        ) {
            Text(stringResource(Res.string.button_sign_in))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text(stringResource(Res.string.sign_in_sign_up_prompt))
        }
    }
}
