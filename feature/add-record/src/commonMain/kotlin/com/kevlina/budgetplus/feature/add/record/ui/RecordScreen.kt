package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_invite
import budgetplus.core.common.generated.resources.ic_group_add
import budgetplus.core.common.generated.resources.ic_settings
import budgetplus.core.common.generated.resources.review_request_message
import budgetplus.core.common.generated.resources.review_request_no
import budgetplus.core.common.generated.resources.review_request_yes
import budgetplus.core.common.generated.resources.settings_description
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AdaptiveScreen
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.kevlina.budgetplus.feature.add.record.RecordViewModel
import com.kevlina.budgetplus.feature.category.pills.toState
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun RecordScreen(navController: NavController<BookDest>) {

    val vm = metroViewModel<RecordViewModel>()

    var isRequestingReview by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = vm) {
        vm.requestReviewEvent
            .consumeEach { isRequestingReview = true }
            .collect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = null,
            titleContent = { BookSelector(navController) },
            menuActions = {
                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_group_add),
                    description = stringResource(Res.string.cta_invite),
                    onClick = vm::shareJoinLink,
                    modifier = Modifier.onPlaced {
                        vm.highlightInviteButton(
                            BubbleDest.Invite(
                                size = it.size,
                                offset = it::positionInRoot
                            )
                        )
                    }
                )

                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    description = stringResource(Res.string.settings_description),
                    onClick = {
                        navController.navigate(BookDest.Settings())
                    }
                )
            }
        )

        AdaptiveScreen(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .weight(1F),
            regularContent = {
                val recordInfoState = vm.toState(
                    navController = navController,
                    scrollable = true,
                )
                RecordContentRegular(recordInfoState)
            },
            wideContent = {
                val recordInfoState = vm.toState(
                    navController = navController,
                    scrollable = true,
                )
                RecordContentWide(recordInfoState)
            },
            packedContent = {
                val recordInfoState = vm.toState(
                    navController = navController,
                    scrollable = false,
                )
                RecordContentPacked(recordInfoState)
            },
            extraContent = {
                DoneAnimator(eventTrigger = vm.recordEvent)
            }
        )

        if (isRequestingReview) {
            ConfirmDialog(
                message = stringResource(Res.string.review_request_message),
                confirmText = stringResource(Res.string.review_request_yes),
                cancelText = stringResource(Res.string.review_request_no),
                onConfirm = {
                    vm.launchReviewFlow()
                    isRequestingReview = false
                },
                onDismiss = {
                    vm.rejectReview()
                    isRequestingReview = false
                }
            )
        }
    }

    // Handle the runtime notification permission request
    NotificationPermissionHandler()
}

@Stable
internal class RecordContentState(
    val recordInfoState: RecordInfoState,
    val calculatorState: CalculatorState,
) {
    companion object {
        val preview = RecordContentState(
            recordInfoState = RecordInfoState.preview,
            calculatorState = CalculatorState.preview,
        )
    }
}

private fun RecordViewModel.toState(
    navController: NavController<BookDest>,
    scrollable: Boolean,
) = RecordContentState(
    recordInfoState = RecordInfoState(
        type = type,
        note = note,
        setType = ::setType,
        scrollable = scrollable,
        categoriesGridState = categoriesVm.toState(
            type = type,
            onEditClicked = if (bookRepo.canEdit) {
                { navController.navigate(BookDest.EditCategory(type.value)) }
            } else null,
        ),
        dateAndPricingState = DateAndPricingState(
            recordDate = recordDate,
            currencySymbol = bookRepo.currencySymbol,
            priceText = calculatorVm.priceText,
            scrollable = scrollable,
            setDate = ::setDate,
            editCurrency = {
                if (bookRepo.canEdit) {
                    navController.navigate(BookDest.CurrencyPicker)
                }
            }
        ),
    ),
    calculatorState = calculatorVm.toState(),
)