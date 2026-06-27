package com.kevlina.budgetplus.feature.welcome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.welcome_join_instruction
import com.kevlina.budgetplus.core.lottie.loadLottieSpec
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.thenIf
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CollabBlock(
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false,
    applyNavBarPadding: Boolean = true,
) {
    val composition by rememberLottieComposition { loadLottieSpec("img_collab") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(LocalAppColors.current.primary)
            .thenIf(applyStatusBarPadding) { Modifier.statusBarsPadding() }
            .thenIf(applyNavBarPadding) { Modifier.navigationBarsPadding() }
    ) {

        Text(
            text = stringResource(Res.string.welcome_join_instruction),
            color = LocalAppColors.current.light,
            fontSize = FontSize.SemiLarge,
            lineHeight = 24.sp,
            modifier = Modifier
                .weight(1F)
                .width(AppTheme.containerMaxWidth)
                .padding(vertical = 24.dp, horizontal = 16.dp)
        )

        Image(
            painter = rememberLottiePainter(composition),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.height(200.dp)
        )
    }
}

@Preview(heightDp = 360)
@Composable
private fun CollabBlock_Preview() = AppTheme(themeColors = ThemeColors.AzureCoast) {
    CollabBlock()
}