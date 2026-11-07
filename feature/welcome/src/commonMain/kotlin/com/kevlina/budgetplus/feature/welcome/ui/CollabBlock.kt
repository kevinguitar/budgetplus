package com.kevlina.budgetplus.feature.welcome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.img_welcome
import budgetplus.core.common.generated.resources.welcome_join_instruction
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.safeArea
import com.kevlina.budgetplus.core.ui.thenIf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CollabBlock(
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false,
    applyNavBarPadding: Boolean = true,
    horizontalInsets: WindowInsetsSides = WindowInsetsSides.Horizontal,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(LocalAppColors.current.primary)
            .thenIf(applyStatusBarPadding) { Modifier.statusBarsPadding() }
            .thenIf(applyNavBarPadding) { Modifier.navigationBarsPadding() }
            .windowInsetsPadding(WindowInsets.safeArea.only(horizontalInsets))
    ) {

        Text(
            text = stringResource(Res.string.welcome_join_instruction),
            color = LocalAppColors.current.light,
            fontSize = FontSize.SemiLarge,
            lineHeight = 24.sp,
            modifier = Modifier
                .width(AppTheme.containerMaxWidth)
                .padding(vertical = 24.dp, horizontal = 16.dp)
        )

        Box(modifier = Modifier.weight(1F)) {
            Image(
                painter = painterResource(Res.drawable.img_welcome),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview(heightDp = 360)
@Composable
private fun CollabBlock_Preview() = AppTheme(themeColors = ThemeColors.AzureCoast) {
    CollabBlock()
}