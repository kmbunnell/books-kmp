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
import com.example.books_kmp.viewmodel.AuthIntent
import com.example.books_kmp.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

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
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        navController.navigate(NavDestination.Library.route) {
                            popUpTo(NavDestination.SignIn.route) { inclusive = true }
                        }
                    }
                }
                SignInScreen(
                    uiState = uiState,
                    effects = authViewModel.effects,
                    onSignIn = { email, password ->
                        authViewModel.onIntent(AuthIntent.SignInWithEmail(email, password))
                    },
                    onNavigateToSignUp = { navController.navigate(NavDestination.SignUp.route) },
                )
            }
            composable(NavDestination.SignUp.route) {
                SignUpScreen()
            }
            composable(NavDestination.Library.route) {
                LibraryScreen()
            }
        }
    }
}
