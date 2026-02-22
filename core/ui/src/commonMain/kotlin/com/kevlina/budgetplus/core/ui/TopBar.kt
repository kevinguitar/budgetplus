package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_back
import budgetplus.core.common.generated.resources.ic_arrow_back
import budgetplus.core.common.generated.resources.ic_drive_file_rename_outline
import budgetplus.core.common.generated.resources.ic_group_add
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

const val TOP_BAR_DARKEN_FACTOR = 0.8F

@Composable
fun TopBar(
    title: String?,
    titleContent: @Composable (() -> Unit)? = null,
    navigateUp: (() -> Unit)? = null,
    menuActions: @Composable (() -> Unit)? = null,
    applyStatusBarPadding: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color = LocalAppColors.current.primary.darken(TOP_BAR_DARKEN_FACTOR))
            .thenIf(applyStatusBarPadding) { Modifier.statusBarsPadding() }
            .fillMaxWidth()
            .height(56.dp)
    ) {

        if (navigateUp != null) {
            IconButton(onClick = navigateUp) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.cta_back),
                    tint = LocalAppColors.current.light
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        when {
            title != null -> Text(
                text = title,
                fontSize = FontSize.Header,
                fontWeight = FontWeight.SemiBold,
                color = LocalAppColors.current.light,
                modifier = Modifier.weight(1F)
            )

            titleContent != null -> Box(
                modifier = Modifier.weight(1F),
            ) {
                titleContent()
            }
        }

        menuActions?.invoke()
    }
}

@Preview
@Composable
private fun TopBar_Preview() = AppTheme {
    TopBar(title = "Money Book")
}

@Preview
@Composable
private fun TopBarWithBack_Preview() = AppTheme {
    TopBar(
        title = "Money Book",
        navigateUp = {},
        menuActions = {
            MenuAction(imageVector = vectorResource(Res.drawable.ic_drive_file_rename_outline), description = "")
            MenuAction(imageVector = vectorResource(Res.drawable.ic_group_add), description = "")
        }
    )
}