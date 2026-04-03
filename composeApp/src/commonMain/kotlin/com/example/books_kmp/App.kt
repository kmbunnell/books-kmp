package com.example.books_kmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.books_kmp.ui.auth.SignInScreen
import com.example.books_kmp.ui.auth.SplashScreen
import com.example.books_kmp.ui.library.LibraryScreen
import com.example.books_kmp.viewmodel.AuthViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = koinInject()
        val uiState by authViewModel.uiState.collectAsState()

        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") {
                SplashScreen(
                    uiState = uiState,
                    onAuthenticated = {
                        navController.navigate("library") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onNotAuthenticated = {
                        navController.navigate("sign_in") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                )
            }
            composable("sign_in") {
                SignInScreen()
            }
            composable("library") {
                LibraryScreen()
            }
        }
    }
}
