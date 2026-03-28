package com.kevlina.budgetplus.feature.record.card

import androidx.compose.runtime.Composable
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.batch_record_delete_confirmation
import budgetplus.core.common.generated.resources.record_confirm_delete
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.isBatched
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteRecordDialog(
    editRecord: Record,
    onDismiss: () -> Unit,
    vm: EditRecordViewModel = metroViewModel(),
) {
    if (editRecord.isBatched) {
        EditBatchDialog(
            onDismiss = onDismiss,
            text = stringResource(Res.string.batch_record_delete_confirmation),
            onSelectOne = {
                vm.deleteRecord(editRecord)
                onDismiss()
            },
            onSelectAll = {
                vm.deleteRecord(editRecord, deleteBatch = true)
                onDismiss()
            }
        )
    } else {
        ConfirmDialog(
            message = stringResource(Res.string.record_confirm_delete),
            onConfirm = {
                vm.deleteRecord(editRecord)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}