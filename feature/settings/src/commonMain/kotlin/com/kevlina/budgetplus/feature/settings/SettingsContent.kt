package com.kevlina.budgetplus.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.batch_record_title
import budgetplus.core.common.generated.resources.book_name_title
import budgetplus.core.common.generated.resources.color_tone_picker_title
import budgetplus.core.common.generated.resources.cta_rename
import budgetplus.core.common.generated.resources.ic_account_circle
import budgetplus.core.common.generated.resources.ic_color_lens
import budgetplus.core.common.generated.resources.ic_currency_exchange
import budgetplus.core.common.generated.resources.ic_delete
import budgetplus.core.common.generated.resources.ic_directions_run
import budgetplus.core.common.generated.resources.ic_edit_note
import budgetplus.core.common.generated.resources.ic_forward_to_inbox
import budgetplus.core.common.generated.resources.ic_instagram
import budgetplus.core.common.generated.resources.ic_language
import budgetplus.core.common.generated.resources.ic_lock_person
import budgetplus.core.common.generated.resources.ic_logout
import budgetplus.core.common.generated.resources.ic_privacy_tip
import budgetplus.core.common.generated.resources.ic_share
import budgetplus.core.common.generated.resources.ic_show_chart
import budgetplus.core.common.generated.resources.ic_star
import budgetplus.core.common.generated.resources.ic_supervised_user_circle
import budgetplus.core.common.generated.resources.ic_vibration
import budgetplus.core.common.generated.resources.premium_hide_ads
import budgetplus.core.common.generated.resources.settings_allow_members_edit
import budgetplus.core.common.generated.resources.settings_allow_members_edit_desc
import budgetplus.core.common.generated.resources.settings_bar_chart
import budgetplus.core.common.generated.resources.settings_chart_mode
import budgetplus.core.common.generated.resources.settings_confirm_delete
import budgetplus.core.common.generated.resources.settings_confirm_leave
import budgetplus.core.common.generated.resources.settings_contact_us
import budgetplus.core.common.generated.resources.settings_delete_book
import budgetplus.core.common.generated.resources.settings_edit_book_currency
import budgetplus.core.common.generated.resources.settings_follow_on_instagram
import budgetplus.core.common.generated.resources.settings_input_vibration
import budgetplus.core.common.generated.resources.settings_language
import budgetplus.core.common.generated.resources.settings_leave_book
import budgetplus.core.common.generated.resources.settings_logout
import budgetplus.core.common.generated.resources.settings_pie_chart
import budgetplus.core.common.generated.resources.settings_privacy_policy
import budgetplus.core.common.generated.resources.settings_rate_us
import budgetplus.core.common.generated.resources.settings_rename_book
import budgetplus.core.common.generated.resources.settings_rename_user
import budgetplus.core.common.generated.resources.settings_share_app
import budgetplus.core.common.generated.resources.settings_view_members
import budgetplus.core.common.generated.resources.username_title
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.settings.api.ChartMode
import com.kevlina.budgetplus.core.settings.api.icon
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.InputDialog
import com.kevlina.budgetplus.core.ui.Switch
import com.kevlina.budgetplus.core.ui.containerPadding
import com.kevlina.budgetplus.feature.settings.member.MembersDialog
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

// A hack to recompose iOS UI when the language is changed
internal val languageSelectionState = mutableStateOf<String?>(null)

@Composable
internal fun SettingsContent(
    navController: NavController<BookDest>,
    vm: SettingsViewModel,
    scrollState: ScrollState,
    showMembers: Boolean,
    modifier: Modifier = Modifier,
) {
    val isBookOwner by vm.isBookOwner.collectAsStateWithLifecycle()
    val allowMembersEdit by vm.allowMembersEdit.collectAsStateWithLifecycle()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()
    val vibrateOnInput by vm.vibrator.vibrateOnInput.collectAsStateWithLifecycle()
    val chartMode by vm.chartModel.chartMode.collectAsStateWithLifecycle()

    var isRenameUserDialogShown by remember { mutableStateOf(false) }
    var isRenameBookDialogShown by remember { mutableStateOf(false) }
    var isMembersDialogShown by rememberSaveable { mutableStateOf(showMembers) }
    var isDeleteOrLeaveDialogShown by remember { mutableStateOf(false) }
    var isChartModeDropdownShown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .containerPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Content section
        SettingsItem(
            text = stringResource(Res.string.batch_record_title),
            showCrownAnimation = true,
            roundTop = true,
            onClick = {
                vm.trackBatchRecordClicked()
                if (isPremium) {
                    navController.navigate(BookDest.BatchRecord)
                } else {
                    navController.navigate(BookDest.UnlockPremium)
                }
            }
        )

        if (!isPremium) {
            SettingsItem(
                text = stringResource(Res.string.premium_hide_ads),
                showCrownAnimation = true,
                onClick = { navController.navigate(BookDest.UnlockPremium) }
            )
        }

        SettingsItem(
            text = stringResource(Res.string.settings_rename_user),
            icon = vectorResource(Res.drawable.ic_account_circle),
            onClick = { isRenameUserDialogShown = true }
        )

        if (vm.canEditBook) {
            SettingsItem(
                text = stringResource(Res.string.settings_rename_book),
                icon = vectorResource(Res.drawable.ic_edit_note),
                onClick = { isRenameBookDialogShown = true }
            )

            SettingsItem(
                text = stringResource(Res.string.settings_edit_book_currency),
                icon = vectorResource(Res.drawable.ic_currency_exchange),
                onClick = { navController.navigate(BookDest.CurrencyPicker) }
            )
        }

        SettingsItem(
            text = stringResource(Res.string.settings_view_members),
            icon = vectorResource(Res.drawable.ic_supervised_user_circle),
            roundBottom = !isBookOwner,
            onClick = { isMembersDialogShown = true }
        )

        if (isBookOwner) {
            SettingsItem(
                text = stringResource(Res.string.settings_allow_members_edit),
                description = stringResource(Res.string.settings_allow_members_edit_desc),
                icon = vectorResource(Res.drawable.ic_lock_person),
                roundBottom = true,
                action = {
                    Switch(
                        checked = allowMembersEdit,
                        onCheckedChange = vm::setAllowMembersEdit,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                },
                onClick = { vm.setAllowMembersEdit(!allowMembersEdit) }
            )
        }

        // General section
        if (canSelectLanguage) {
            SettingsItem(
                text = stringResource(Res.string.settings_language),
                icon = vectorResource(Res.drawable.ic_language),
                roundTop = true,
                onClick = {
                    vm.navigation.openLanguageSettings { languageCode ->
                        Logger.i("SettingsScreen provides: $languageCode")
                        languageSelectionState.value = languageCode
                    }
                }
            )
        }

        SettingsItem(
            text = stringResource(Res.string.color_tone_picker_title),
            icon = vectorResource(Res.drawable.ic_color_lens),
            roundTop = !canSelectLanguage,
            onClick = { navController.navigate(BookDest.Colors()) }
        )

        SettingsItem(
            text = stringResource(Res.string.settings_input_vibration),
            icon = vectorResource(Res.drawable.ic_vibration),
            verticalPadding = 4.dp,
            action = {
                val hapticFeedback = LocalHapticFeedback.current

                Switch(
                    checked = vibrateOnInput,
                    onCheckedChange = { checked ->
                        val feedbackType = if (checked) {
                            HapticFeedbackType.ToggleOn
                        } else {
                            HapticFeedbackType.ToggleOff
                        }
                        hapticFeedback.performHapticFeedback(feedbackType)
                        vm.vibrator.toggleVibrateOnInput()
                    },
                    modifier = Modifier.padding(end = 10.dp)
                )
            },
            onClick = vm.vibrator::toggleVibrateOnInput
        )

        SettingsItem(
            text = stringResource(Res.string.settings_chart_mode),
            icon = vectorResource(Res.drawable.ic_show_chart),
            onClick = { isChartModeDropdownShown = true },
            action = {
                Box {
                    Image(
                        imageVector = chartMode.icon,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(LocalAppColors.current.dark),
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    DropdownMenu(
                        expanded = isChartModeDropdownShown,
                        onDismissRequest = { isChartModeDropdownShown = false }
                    ) {
                        DropdownItem(
                            name = stringResource(Res.string.settings_bar_chart),
                            icon = ChartMode.BarChart.icon,
                            onClick = {
                                vm.chartModel.setChartMode(ChartMode.BarChart)
                                isChartModeDropdownShown = false
                            }
                        )

                        DropdownItem(
                            name = stringResource(Res.string.settings_pie_chart),
                            icon = ChartMode.PieChart.icon,
                            onClick = {
                                vm.chartModel.setChartMode(ChartMode.PieChart)
                                isChartModeDropdownShown = false
                            }
                        )
                    }
                }
            }
        )

        SettingsItem(
            text = stringResource(Res.string.settings_share_app),
            icon = vectorResource(Res.drawable.ic_share),
            onClick = vm.navigation::share
        )

        SettingsItem(
            text = stringResource(Res.string.settings_rate_us),
            icon = vectorResource(Res.drawable.ic_star),
            onClick = vm.navigation::rateUs
        )

        SettingsItem(
            text = stringResource(Res.string.settings_follow_on_instagram),
            drawableRes = Res.drawable.ic_instagram,
            onClick = vm.navigation::followOnInstagram
        )

        SettingsItem(
            text = stringResource(Res.string.settings_contact_us),
            icon = vectorResource(Res.drawable.ic_forward_to_inbox),
            onClick = vm.navigation::contactUs
        )

        SettingsItem(
            text = stringResource(Res.string.settings_privacy_policy),
            icon = vectorResource(Res.drawable.ic_privacy_tip),
            roundBottom = true,
            onClick = vm.navigation::viewPrivacyPolicy
        )

        // Danger section
        SettingsItem(
            text = stringResource(if (isBookOwner) {
                Res.string.settings_delete_book
            } else {
                Res.string.settings_leave_book
            }),
            icon = if (isBookOwner) {
                vectorResource(Res.drawable.ic_delete)
            } else {
                vectorResource(Res.drawable.ic_directions_run)
            },
            roundTop = true,
            onClick = { isDeleteOrLeaveDialogShown = true }
        )

        SettingsItem(
            text = stringResource(Res.string.settings_logout),
            icon = vectorResource(Res.drawable.ic_logout),
            roundBottom = true,
            onClick = vm.navigation::logout
        )
    }

    if (isRenameUserDialogShown) {
        InputDialog(
            currentInput = vm.currentUsername,
            title = stringResource(Res.string.username_title),
            buttonText = stringResource(Res.string.cta_rename),
            onButtonClicked = vm::renameUser,
            onDismiss = { isRenameUserDialogShown = false }
        )
    }

    if (isRenameBookDialogShown) {
        InputDialog(
            currentInput = vm.currentBookName,
            title = stringResource(Res.string.book_name_title),
            buttonText = stringResource(Res.string.cta_rename),
            onButtonClicked = vm::renameBook,
            onDismiss = { isRenameBookDialogShown = false }
        )
    }

    if (isMembersDialogShown) {
        MembersDialog(
            onDismiss = { isMembersDialogShown = false }
        )
    }

    if (isDeleteOrLeaveDialogShown) {
        ConfirmDialog(
            message = stringResource(
                if (isBookOwner) Res.string.settings_confirm_delete else Res.string.settings_confirm_leave,
                vm.currentBookName.orEmpty()
            ),
            onConfirm = {
                vm.deleteOrLeave()
                isDeleteOrLeaveDialogShown = false
            },
            onDismiss = { isDeleteOrLeaveDialogShown = false }
        )
    }
}