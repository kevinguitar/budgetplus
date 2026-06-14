package com.kevlina.budgetplus.feature.record.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import budgetplus.core.common.generated.resources.batch_record_all_future_records
import budgetplus.core.common.generated.resources.batch_record_edit_confirmation
import budgetplus.core.common.generated.resources.batch_record_only_this
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.ButtonRow
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditBatchDialog(
    onDismiss: () -> Unit,
    text: String,
    onSelectOne: () -> Unit,
    onSelectAll: () -> Unit,
) {
    AppDialog(onDismissRequest = onDismiss) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
        ) {

            Text(
                text = text,
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            ButtonRow {
                Button(onClick = onSelectOne) {
                    Text(
                        text = stringResource(Res.string.batch_record_only_this),
                        color = LocalAppColors.current.light,
                    )
                }

                Button(onClick = onSelectAll) {
                    Text(
                        text = stringResource(Res.string.batch_record_all_future_records),
                        color = LocalAppColors.current.light,
                    )
                }
            }
        }
    }
}

@Preview(locale = "ja")
@Preview(locale = "en")
@Composable
private fun EditBatchDialog_Preview() = AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        EditBatchDialog(
            onDismiss = { },
            text = stringResource(Res.string.batch_record_edit_confirmation),
            onSelectOne = { },
            onSelectAll = {}
        )
    }
}