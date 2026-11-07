package com.kevlina.budgetplus.feature.welcome.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.ui.AppTheme

@Composable
fun WelcomeContentWide(state: WelcomeState) {
    val isCreatingBook by state.isCreatingBook.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        CreateBookBlock(
            bookName = state.bookName,
            isCreatingBook = isCreatingBook,
            createBook = state.createBook,
            isWideMode = true,
            applyStatusBarPadding = true,
            applyNavBarPadding = true,
            // In wide mode the blocks are side by side, so each one only avoids the insets on its outer edge.
            horizontalInsets = WindowInsetsSides.Start,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
        )

        CollabBlock(
            applyStatusBarPadding = true,
            applyNavBarPadding = true,
            horizontalInsets = WindowInsetsSides.End,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
        )
    }
}

@Preview(showBackground = true, widthDp = 720, heightDp = 400)
@Composable
private fun WelcomeContentWide_Preview() = AppTheme {
    WelcomeContentWide(WelcomeState.preview)
}
