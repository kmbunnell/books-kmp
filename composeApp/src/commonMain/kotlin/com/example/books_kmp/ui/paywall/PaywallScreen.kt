package com.example.books_kmp.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bookskmp.composeapp.generated.resources.Res
import bookskmp.composeapp.generated.resources.cd_navigate_up
import bookskmp.composeapp.generated.resources.paywall_button_downgrade
import bookskmp.composeapp.generated.resources.paywall_button_go_premium
import bookskmp.composeapp.generated.resources.paywall_confirmation_downgraded
import bookskmp.composeapp.generated.resources.paywall_confirmation_upgraded
import bookskmp.composeapp.generated.resources.paywall_error_update_failed
import bookskmp.composeapp.generated.resources.paywall_tier_free
import bookskmp.composeapp.generated.resources.paywall_tier_premium
import bookskmp.composeapp.generated.resources.paywall_title
import com.example.books_kmp.ui.TestTags
import com.example.books_kmp.ui.components.AppSnackbarHost
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaywallScreen(onNavigateUp: () -> Unit = {}) {
    val viewModel: PaywallViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaywallScreenContent(
        uiState = uiState,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreenContent(
    uiState: PaywallUiState,
    effects: Flow<PaywallEffect>,
    onIntent: (PaywallIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val upgradedMessage = stringResource(Res.string.paywall_confirmation_upgraded)
    val downgradedMessage = stringResource(Res.string.paywall_confirmation_downgraded)
    val updateFailedMessage = stringResource(Res.string.paywall_error_update_failed)

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            val message =
                when (effect) {
                    is PaywallEffect.ShowConfirmation ->
                        if (effect.isPremium) upgradedMessage else downgradedMessage
                    is PaywallEffect.ShowError ->
                        when (effect.error) {
                            PaywallError.UpdateFailed -> updateFailedMessage
                        }
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.paywall_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text =
                    stringResource(
                        if (uiState.isPremium) Res.string.paywall_tier_premium else Res.string.paywall_tier_free,
                    ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(TestTags.Paywall.TierLabel),
            )

            if (uiState.isPremium) {
                Button(
                    onClick = { onIntent(PaywallIntent.Downgrade) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.testTag(TestTags.Paywall.DowngradeButton),
                ) {
                    Text(stringResource(Res.string.paywall_button_downgrade))
                }
            } else {
                Button(
                    onClick = { onIntent(PaywallIntent.GoPremium) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.testTag(TestTags.Paywall.GoPremiumButton),
                ) {
                    Text(stringResource(Res.string.paywall_button_go_premium))
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(TestTags.Paywall.LoadingIndicator),
                )
            }
        }
    }
}
