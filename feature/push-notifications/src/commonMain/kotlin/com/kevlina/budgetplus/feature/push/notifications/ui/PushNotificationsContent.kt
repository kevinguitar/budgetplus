package com.kevlina.budgetplus.feature.push.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_selection
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import budgetplus.core.common.generated.resources.push_notif_deeplink
import budgetplus.core.common.generated.resources.push_notif_language_en
import budgetplus.core.common.generated.resources.push_notif_language_ja
import budgetplus.core.common.generated.resources.push_notif_language_zh_cn
import budgetplus.core.common.generated.resources.push_notif_language_zh_tw
import budgetplus.core.common.generated.resources.push_notif_send_to_everyone
import budgetplus.core.common.generated.resources.push_notif_send_to_everyone_confirmation
import budgetplus.core.common.generated.resources.push_notif_send_to_internal_topic
import budgetplus.core.common.generated.resources.push_notif_target_audience
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import com.kevlina.budgetplus.core.ui.containerPadding
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.kevlina.budgetplus.feature.push.notifications.AudienceTarget
import com.kevlina.budgetplus.feature.push.notifications.PushNotificationsViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun PushNotificationsContent(
    navigateUp: () -> Unit,
) {

    val vm = metroViewModel<PushNotificationsViewModel>()

    val audienceTarget by vm.audienceTarget.collectAsStateWithLifecycle()
    val sendToCn by vm.sendToCn.collectAsStateWithLifecycle()
    val sendToJa by vm.sendToJa.collectAsStateWithLifecycle()
    val sendToEn by vm.sendToEn.collectAsStateWithLifecycle()

    var isConfirmationDialogShown by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .containerPadding()
            .padding(16.dp)
            .imePadding()
    ) {
        LanguageBlock(
            textRes = Res.string.push_notif_language_zh_tw,
            title = vm.titleTw,
            description = vm.descTw,
            isOptional = false
        )

        LanguageBlock(
            textRes = Res.string.push_notif_language_zh_cn,
            title = vm.titleCn,
            description = vm.descCn,
            enabled = sendToCn,
            onEnableUpdate = { vm.sendToCn.value = it }
        )

        LanguageBlock(
            textRes = Res.string.push_notif_language_ja,
            title = vm.titleJa,
            description = vm.descJa,
            enabled = sendToJa,
            onEnableUpdate = { vm.sendToJa.value = it }
        )

        LanguageBlock(
            textRes = Res.string.push_notif_language_en,
            title = vm.titleEn,
            description = vm.descEn,
            enabled = sendToEn,
            onEnableUpdate = { vm.sendToEn.value = it }
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.push_notif_target_audience),
                fontSize = FontSize.Large,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1F))

            Box {
                var isSelectorShown by remember { mutableStateOf(false) }

                Row(modifier = Modifier.rippleClick { isSelectorShown = true }) {
                    Text(
                        text = stringResource(audienceTarget.toStringRes()),
                        fontSize = FontSize.Large,
                    )

                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(Res.string.book_selection),
                        tint = LocalAppColors.current.dark
                    )
                }

                DropdownMenu(
                    expanded = isSelectorShown,
                    onDismissRequest = { isSelectorShown = false },
                    offset = DpOffset(0.dp, 8.dp),
                ) {
                    AudienceTarget.entries.forEach { target ->
                        DropdownItem(onClick = {
                            vm.audienceTarget.value = target
                            isSelectorShown = false
                        }) {
                            Text(
                                text = stringResource(target.toStringRes()),
                                fontSize = FontSize.SemiLarge
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(Res.string.push_notif_deeplink),
            fontSize = FontSize.Large,
            fontWeight = FontWeight.SemiBold
        )

        TextField(
            state = vm.deeplinkPath,
            title = "",
            placeholder = "overview"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = vm::sendToInternalTopic,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.push_notif_send_to_internal_topic),
                color = LocalAppColors.current.light,
                fontSize = FontSize.Large,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 24.dp)
            )
        }

        Button(
            onClick = { isConfirmationDialogShown = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.push_notif_send_to_everyone),
                color = LocalAppColors.current.light,
                fontSize = FontSize.Large,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 24.dp)
            )
        }
    }

    if (isConfirmationDialogShown) {
        ConfirmDialog(
            message = stringResource(Res.string.push_notif_send_to_everyone_confirmation),
            onConfirm = {
                vm.sendToEveryone()
                isConfirmationDialogShown = false
                navigateUp()
            },
            onDismiss = { isConfirmationDialogShown = false }
        )
    }
}