package com.example.books_kmp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.button_create_account
import bookskmp.composeapp.generated.resources.cd_hide_password
import bookskmp.composeapp.generated.resources.cd_show_password
import bookskmp.composeapp.generated.resources.error_confirm_password_required
import bookskmp.composeapp.generated.resources.error_email_already_in_use
import bookskmp.composeapp.generated.resources.error_email_required
import bookskmp.composeapp.generated.resources.error_invalid_email_format
import bookskmp.composeapp.generated.resources.error_password_required
import bookskmp.composeapp.generated.resources.error_passwords_do_not_match
import bookskmp.composeapp.generated.resources.error_rate_limited
import bookskmp.composeapp.generated.resources.error_sign_up_failed
import bookskmp.composeapp.generated.resources.error_weak_password
import bookskmp.composeapp.generated.resources.hint_password_requirement
import bookskmp.composeapp.generated.resources.label_confirm_password
import bookskmp.composeapp.generated.resources.label_email
import bookskmp.composeapp.generated.resources.label_password
import bookskmp.composeapp.generated.resources.sign_up_sign_in_prompt
import com.example.books_kmp.domain.auth.SignUpError
import com.example.books_kmp.ui.TestTags
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(onNavigateToSignIn: () -> Unit) {
    val viewModel: SignUpViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onSignUp = { email, password, confirmPassword ->
            viewModel.onIntent(SignUpIntent.SignUp(email, password, confirmPassword))
        },
        onNavigateToSignIn = onNavigateToSignIn,
    )
}

@Composable
fun SignUpScreenContent(
    uiState: SignUpUiState,
    effects: SharedFlow<SignUpEffect>,
    onSignUp: (email: String, password: String, confirmPassword: String) -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val errorEmailRequired = stringResource(Res.string.error_email_required)
    val errorInvalidEmailFormat = stringResource(Res.string.error_invalid_email_format)
    val errorPasswordRequired = stringResource(Res.string.error_password_required)
    val errorWeakPassword = stringResource(Res.string.error_weak_password)
    val errorConfirmPasswordRequired = stringResource(Res.string.error_confirm_password_required)
    val errorPasswordsDoNotMatch = stringResource(Res.string.error_passwords_do_not_match)
    val errorEmailAlreadyInUse = stringResource(Res.string.error_email_already_in_use)
    val errorRateLimited = stringResource(Res.string.error_rate_limited)
    val errorSignUpFailed = stringResource(Res.string.error_sign_up_failed)
    val cdShowPassword = stringResource(Res.string.cd_show_password)
    val cdHidePassword = stringResource(Res.string.cd_hide_password)
    val hintPasswordRequirement = stringResource(Res.string.hint_password_requirement)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is SignUpEffect.ShowError ->
                    snackbarHostState.showSnackbar(
                        when (effect.error) {
                            SignUpError.EmailAlreadyInUse -> errorEmailAlreadyInUse
                            SignUpError.EmailRateLimitExceeded -> errorRateLimited
                            SignUpError.EmptyEmail,
                            SignUpError.EmptyPassword,
                            SignUpError.EmptyConfirmPassword,
                            SignUpError.PasswordMismatch,
                            SignUpError.WeakPassword,
                            SignUpError.InvalidEmail,
                            SignUpError.SignUpFailed -> errorSignUpFailed
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
            supportingText =
                uiState.emailError?.let { error ->
                    {
                        Text(
                            when (error) {
                                SignUpError.InvalidEmail -> errorInvalidEmailFormat
                                else -> errorEmailRequired
                            },
                        )
                    }
                },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignUp.EmailField),
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
                    modifier = Modifier.testTag(TestTags.SignUp.PasswordToggle),
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
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() }),
            isError = uiState.passwordError != null,
            supportingText =
                uiState.passwordError?.let { error ->
                    {
                        Text(
                            when (error) {
                                SignUpError.WeakPassword -> errorWeakPassword
                                else -> errorPasswordRequired
                            },
                        )
                    }
                },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .testTag(TestTags.SignUp.PasswordField),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(Res.string.label_confirm_password)) },
            singleLine = true,
            visualTransformation =
                if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    modifier = Modifier.testTag(TestTags.SignUp.ConfirmPasswordToggle),
                ) {
                    Icon(
                        imageVector =
                            if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (confirmPasswordVisible) cdHidePassword else cdShowPassword,
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
                    onDone = { onSignUp(email, password, confirmPassword) },
                ),
            isError = uiState.confirmPasswordError != null,
            supportingText =
                uiState.confirmPasswordError?.let { error ->
                    {
                        Text(
                            when (error) {
                                SignUpError.EmptyConfirmPassword -> errorConfirmPasswordRequired
                                else -> errorPasswordsDoNotMatch
                            },
                        )
                    }
                },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasswordFocusRequester)
                    .testTag(TestTags.SignUp.ConfirmPasswordField),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hintPasswordRequirement,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignUp.PasswordHint),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.testTag(TestTags.SignUp.LoadingIndicator))
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = { onSignUp(email, password, confirmPassword) },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SignUp.CreateAccountButton),
        ) {
            Text(stringResource(Res.string.button_create_account))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToSignIn) {
            Text(stringResource(Res.string.sign_up_sign_in_prompt))
        }
    }
}
