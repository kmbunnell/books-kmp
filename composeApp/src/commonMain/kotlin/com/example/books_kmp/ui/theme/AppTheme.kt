package com.example.books_kmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Semantic palette — names describe role, not light/dark value
private val BooksBrand = Color(0xFF6650A4)
private val BooksOnBrand = Color(0xFFFFFFFF)
private val BooksBrandContainer = Color(0xFFEADDFF)
private val BooksOnBrandContainer = Color(0xFF21005D)

private val BooksSecondary = Color(0xFF625B71)
private val BooksOnSecondary = Color(0xFFFFFFFF)
private val BooksSecondaryContainer = Color(0xFFE8DEF8)
private val BooksOnSecondaryContainer = Color(0xFF1D192B)

private val BooksError = Color(0xFFB3261E)
private val BooksOnError = Color(0xFFFFFFFF)
private val BooksErrorContainer = Color(0xFFF9DEDC)
private val BooksOnErrorContainer = Color(0xFF410E0B)

private val BooksBackground = Color(0xFFFFFBFE)
private val BooksOnBackground = Color(0xFF1C1B1F)

private val BooksSurface = Color(0xFFFFFBFE)
private val BooksOnSurface = Color(0xFF1C1B1F)
private val BooksSurfaceVariant = Color(0xFFE7E0EC)
private val BooksOnSurfaceVariant = Color(0xFF49454F)
private val BooksOutline = Color(0xFF79747E)

// Dark-mode overrides for the same semantic roles
private val BooksBrandDark = Color(0xFFD0BCFF)
private val BooksOnBrandDark = Color(0xFF381E72)
private val BooksBrandContainerDark = Color(0xFF4F378B)
private val BooksOnBrandContainerDark = Color(0xFFEADDFF)

private val BooksSecondaryDark = Color(0xFFCCC2DC)
private val BooksOnSecondaryDark = Color(0xFF332D41)
private val BooksSecondaryContainerDark = Color(0xFF4A4458)
private val BooksOnSecondaryContainerDark = Color(0xFFE8DEF8)

private val BooksErrorDark = Color(0xFFF2B8B5)
private val BooksOnErrorDark = Color(0xFF601410)
private val BooksErrorContainerDark = Color(0xFF8C1D18)
private val BooksOnErrorContainerDark = Color(0xFFF9DEDC)

private val BooksBackgroundDark = Color(0xFF1C1B1F)
private val BooksOnBackgroundDark = Color(0xFFE6E1E5)

private val BooksSurfaceDark = Color(0xFF1C1B1F)
private val BooksOnSurfaceDark = Color(0xFFE6E1E5)
private val BooksSurfaceVariantDark = Color(0xFF49454F)
private val BooksOnSurfaceVariantDark = Color(0xFFCAC4D0)
private val BooksOutlineDark = Color(0xFF938F99)

val BooksLightColorScheme =
    lightColorScheme(
        primary = BooksBrand,
        onPrimary = BooksOnBrand,
        primaryContainer = BooksBrandContainer,
        onPrimaryContainer = BooksOnBrandContainer,
        secondary = BooksSecondary,
        onSecondary = BooksOnSecondary,
        secondaryContainer = BooksSecondaryContainer,
        onSecondaryContainer = BooksOnSecondaryContainer,
        error = BooksError,
        onError = BooksOnError,
        errorContainer = BooksErrorContainer,
        onErrorContainer = BooksOnErrorContainer,
        background = BooksBackground,
        onBackground = BooksOnBackground,
        surface = BooksSurface,
        onSurface = BooksOnSurface,
        surfaceVariant = BooksSurfaceVariant,
        onSurfaceVariant = BooksOnSurfaceVariant,
        outline = BooksOutline,
    )

val BooksDarkColorScheme =
    darkColorScheme(
        primary = BooksBrandDark,
        onPrimary = BooksOnBrandDark,
        primaryContainer = BooksBrandContainerDark,
        onPrimaryContainer = BooksOnBrandContainerDark,
        secondary = BooksSecondaryDark,
        onSecondary = BooksOnSecondaryDark,
        secondaryContainer = BooksSecondaryContainerDark,
        onSecondaryContainer = BooksOnSecondaryContainerDark,
        error = BooksErrorDark,
        onError = BooksOnErrorDark,
        errorContainer = BooksErrorContainerDark,
        onErrorContainer = BooksOnErrorContainerDark,
        background = BooksBackgroundDark,
        onBackground = BooksOnBackgroundDark,
        surface = BooksSurfaceDark,
        onSurface = BooksOnSurfaceDark,
        surfaceVariant = BooksSurfaceVariantDark,
        onSurfaceVariant = BooksOnSurfaceVariantDark,
        outline = BooksOutlineDark,
    )

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BooksDarkColorScheme else BooksLightColorScheme,
        content = content,
    )
}
