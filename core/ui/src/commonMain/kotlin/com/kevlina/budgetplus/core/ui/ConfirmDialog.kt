package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_cancel
import budgetplus.core.common.generated.resources.cta_confirm
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(Res.string.cta_confirm),
    cancelText: String = stringResource(Res.string.cta_cancel),
) {

    AppDialog(onDismissRequest = onDismiss) {

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .padding(all = 8.dp)
        ) {

            Text(
                text = message,
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            ButtonRow {
                Button(onClick = onDismiss) {
                    Text(
                        text = cancelText,
                        color = LocalAppColors.current.light,
                    )
                }

                Button(onClick = onConfirm) {
                    Text(
                        text = confirmText,
                        color = LocalAppColors.current.light,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmDialog_Preview() = AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        ConfirmDialog(
            message = "You won't be able to restore the changes. Are you sure you want to delete this book?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}