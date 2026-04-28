package com.example.books_kmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.books_kmp.ui.addbook.AddBookScreen
import com.example.books_kmp.ui.auth.SignInScreen
import com.example.books_kmp.ui.auth.SignUpScreen
import com.example.books_kmp.ui.auth.SplashScreen
import com.example.books_kmp.ui.bookdetail.BookDetailScreen
import com.example.books_kmp.ui.library.LibraryScreen
import com.example.books_kmp.ui.manualentry.ManualEntryScreen
import com.example.books_kmp.ui.tags.TagManagementScreen
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

        NavHost(navController = navController, startDestination = Route.Splash) {
            composable<Route.Splash> {
                SplashScreen(
                    uiState = uiState,
                    onAuthenticated = {
                        navController.navigate(Route.Library) {
                            popUpTo<Route.Splash> { inclusive = true }
                        }
                    },
                    onNotAuthenticated = {
                        navController.navigate(Route.SignIn) {
                            popUpTo<Route.Splash> { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.SignIn> {
                NavigateToLibraryOnAuth(
                    isAuthenticated = uiState.isAuthenticated,
                    onAuthenticated = {
                        navController.navigate(Route.Library) {
                            popUpTo<Route.SignIn> { inclusive = true }
                        }
                    },
                )
                SignInScreen(
                    onNavigateToSignUp = { navController.navigate(Route.SignUp) },
                )
            }
            composable<Route.SignUp> {
                NavigateToLibraryOnAuth(
                    isAuthenticated = uiState.isAuthenticated,
                    onAuthenticated = {
                        navController.navigate(Route.Library) {
                            popUpTo<Route.SignIn> { inclusive = true }
                        }
                    },
                )
                SignUpScreen(
                    onNavigateToSignIn = { navController.popBackStack() },
                )
            }
            composable<Route.Library> {
                NavigateToSignInOnSignOut(
                    isAuthenticated = uiState.isAuthenticated,
                    isLoading = uiState.isLoading,
                    onSignedOut = {
                        navController.navigate(Route.SignIn) {
                            popUpTo<Route.Library> { inclusive = true }
                        }
                    },
                )
                LibraryScreen(
                    onSignOut = { authViewModel.onIntent(AuthIntent.SignOut) },
                    onNavigateToAddBook = { navController.navigate(Route.AddBook) },
                    onNavigateToTagManagement = { navController.navigate(Route.TagManagement) },
                )
            }
            composable<Route.AddBook> {
                AddBookScreen(
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToManualEntry = {
                        navController.navigate(Route.ManualEntry)
                    },
                )
            }
            composable<Route.ManualEntry> {
                ManualEntryScreen(
                    onNavigateToLibrary = {
                        navController.navigate(Route.Library) {
                            popUpTo<Route.ManualEntry> { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<Route.TagManagement> {
                TagManagementScreen(
                    onNavigateUp = { navController.popBackStack() },
                )
            }
            composable<Route.BookDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.BookDetail>()
                BookDetailScreen(
                    bookId = route.bookId,
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToTagManagement = { navController.navigate(Route.TagManagement) },
                )
            }
        }
    }
}
