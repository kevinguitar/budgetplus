package com.kevlina.budgetplus.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.auth_apple
import budgetplus.core.common.generated.resources.auth_google
import budgetplus.core.common.generated.resources.ic_apple
import budgetplus.core.common.generated.resources.ic_google
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.LocalTypographyScale
import com.kevlina.budgetplus.core.theme.typographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SocialSignInButton(
    provider: SocialSignInProvider,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
            .height(56.dp * LocalTypographyScale.current.scale),
        onClick = onClick,
        color = LocalAppColors.current.light
    ) {
        Image(
            painter = painterResource(
                when (provider) {
                    SocialSignInProvider.Google -> Res.drawable.ic_google
                    SocialSignInProvider.Apple -> Res.drawable.ic_apple
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .typographyScale()
        )

        Text(
            text = stringResource(
                when (provider) {
                    SocialSignInProvider.Google -> Res.string.auth_google
                    SocialSignInProvider.Apple -> Res.string.auth_apple
                }
            ),
            color = LocalAppColors.current.dark,
            fontSize = FontSize.SemiLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

enum class SocialSignInProvider {
    Google, Apple
}

@PreviewFontScale
@Composable
private fun SocialSignInButton_Preview(
    @PreviewParameter(SocialSignInProviderProvider::class) provider: SocialSignInProvider,
) = AppTheme {
    SocialSignInButton(
        provider = provider,
        onClick = { },
    )
}

private class SocialSignInProviderProvider : PreviewParameterProvider<SocialSignInProvider> {
    override val values: Sequence<SocialSignInProvider> = SocialSignInProvider.entries.asSequence()
}