package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold as MaterialScaffold

/**
 *  @param contentWindowInsets the insets for the content padding and the snackbar, null for the
 *  Material default.
 */
@Composable
fun Scaffold(
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    MaterialScaffold(
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        contentWindowInsets = contentWindowInsets ?: ScaffoldDefaults.contentWindowInsets,
        content = content
    )
}
