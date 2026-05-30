package com.example.books_kmp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_sign_in
import bookskmp.composeapp.generated.resources.cd_hide_password
import bookskmp.composeapp.generated.resources.cd_show_password
import bookskmp.composeapp.generated.resources.error_email_not_verified
import bookskmp.composeapp.generated.resources.error_email_required
import bookskmp.composeapp.generated.resources.error_invalid_credentials
import bookskmp.composeapp.generated.resources.error_password_required
import bookskmp.composeapp.generated.resources.error_sign_in_failed
import bookskmp.composeapp.generated.resources.label_email
import bookskmp.composeapp.generated.resources.label_password
import bookskmp.composeapp.generated.resources.sign_in_sign_up_prompt
import com.example.books_kmp.domain.auth.SignInError
import com.example.books_kmp.ui.TestTags
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(onNavigateToSignUp: () -> Unit) {
    val viewModel: SignInViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }

    val errorEmailRequired = stringResource(Res.string.error_email_required)
    val errorPasswordRequired = stringResource(Res.string.error_password_required)
    val errorInvalidCredentials = stringResource(Res.string.error_invalid_credentials)
    val errorEmailNotVerified = stringResource(Res.string.error_email_not_verified)
    val errorSignInFailed = stringResource(Res.string.error_sign_in_failed)
    val cdShowPassword = stringResource(Res.string.cd_show_password)
    val cdHidePassword = stringResource(Res.string.cd_hide_password)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is SignInEffect.ShowError ->
                    snackbarHostState.showSnackbar(
                        when (effect.error) {
                            SignInError.InvalidCredentials -> errorInvalidCredentials
                            SignInError.EmailNotVerified -> errorEmailNotVerified
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
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
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
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag(TestTags.SignIn.PasswordToggle),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) cdHidePassword else cdShowPassword,
                    )
                }
            },
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
                    .focusRequester(passwordFocusRequester)
                    .testTag(TestTags.SignIn.PasswordField),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSignIn(email, password) },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignIn.SignInButton),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).testTag(TestTags.SignIn.LoadingIndicator),
                    strokeWidth = 2.dp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(Res.string.button_sign_in))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text(stringResource(Res.string.sign_in_sign_up_prompt))
        }
    }
}
