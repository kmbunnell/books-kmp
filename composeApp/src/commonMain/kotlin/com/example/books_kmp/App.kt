package com.example.books_kmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.books_kmp.ui.auth.SignInScreen
import com.example.books_kmp.ui.auth.SignUpScreen
import com.example.books_kmp.ui.auth.SplashScreen
import com.example.books_kmp.ui.library.LibraryScreen
import com.example.books_kmp.ui.manualentry.ManualEntryScreen
import com.example.books_kmp.viewmodel.AuthIntent
import com.example.books_kmp.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun NavigateToSignInOnSignOut(
    isAuthenticated: Boolean,
    isLoading: Boolean,
    onSignedOut: () -> Unit,
) {
    LaunchedEffect(isAuthenticated, isLoading) {
        if (!isAuthenticated && !isLoading) {
            onSignedOut()
        }
    }
}

@Composable
private fun NavigateToLibraryOnAuth(
    isAuthenticated: Boolean,
    onAuthenticated: () -> Unit,
) {
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onAuthenticated()
        }
    }
}

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = koinViewModel()
        val uiState by authViewModel.uiState.collectAsState()

        NavHost(navController = navController, startDestination = NavDestination.Splash.route) {
            composable(NavDestination.Splash.route) {
                SplashScreen(
                    uiState = uiState,
                    onAuthenticated = {
                        navController.navigate(NavDestination.Library.route) {
                            popUpTo(NavDestination.Splash.route) { inclusive = true }
                        }
                    },
                    onNotAuthenticated = {
                        navController.navigate(NavDestination.SignIn.route) {
                            popUpTo(NavDestination.Splash.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(NavDestination.SignIn.route) {
                NavigateToLibraryOnAuth(
                    isAuthenticated = uiState.isAuthenticated,
                    onAuthenticated = {
                        navController.navigate(NavDestination.Library.route) {
                            popUpTo(NavDestination.SignIn.route) { inclusive = true }
                        }
                    },
                )
                SignInScreen(
                    onNavigateToSignUp = { navController.navigate(NavDestination.SignUp.route) },
                )
            }
            composable(NavDestination.SignUp.route) {
                NavigateToLibraryOnAuth(
                    isAuthenticated = uiState.isAuthenticated,
                    onAuthenticated = {
                        navController.navigate(NavDestination.Library.route) {
                            popUpTo(NavDestination.SignIn.route) { inclusive = true }
                        }
                    },
                )
                SignUpScreen(
                    onNavigateToSignIn = { navController.popBackStack() },
                )
            }
            composable(NavDestination.Library.route) {
                NavigateToSignInOnSignOut(
                    isAuthenticated = uiState.isAuthenticated,
                    isLoading = uiState.isLoading,
                    onSignedOut = {
                        navController.navigate(NavDestination.SignIn.route) {
                            popUpTo(NavDestination.Library.route) { inclusive = true }
                        }
                    },
                )
                LibraryScreen(onSignOut = { authViewModel.onIntent(AuthIntent.SignOut) })
            }
            composable(NavDestination.ManualEntry.route) {
                ManualEntryScreen(
                    onNavigateToLibrary = {
                        navController.navigate(NavDestination.Library.route) {
                            popUpTo(NavDestination.ManualEntry.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
