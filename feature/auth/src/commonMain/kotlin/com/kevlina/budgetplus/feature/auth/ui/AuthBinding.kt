package com.kevlina.budgetplus.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AdaptiveScreen
import com.kevlina.budgetplus.feature.auth.CommonAuthViewModel

@Composable
fun AuthBinding(
    vm: CommonAuthViewModel,
    signInWithGoogle: () -> Unit,
    signInWithApple: () -> Unit,
) {
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    AdaptiveScreen(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.primary)
            .systemBarsPadding(),
        regularContent = {
            AuthContent(
                signInWithGoogle = signInWithGoogle,
                signInWithApple = signInWithApple,
                isLoading = isLoading
            )
        },
        wideContent = {
            AuthContentWide(
                signInWithGoogle = signInWithGoogle,
                signInWithApple = signInWithApple,
                isLoading = isLoading
            )
        }
    )
}