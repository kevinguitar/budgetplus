package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.export_csv_confirmation
import budgetplus.core.common.generated.resources.export_cta
import budgetplus.core.common.generated.resources.ic_file_download
import budgetplus.core.common.generated.resources.ic_format_list_numbered
import budgetplus.core.common.generated.resources.overview_details_title
import budgetplus.core.common.generated.resources.overview_title
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.shortFormatted
import com.kevlina.budgetplus.core.settings.api.icon
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AdaptiveScreen
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.kevlina.budgetplus.feature.overview.OverviewMode
import com.kevlina.budgetplus.feature.overview.OverviewViewModel
import com.kevlina.budgetplus.feature.overview.shouldRequestStoragePermission
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun OverviewScreen(navController: NavController<BookDest>) {

    val vm = metroViewModel<OverviewViewModel>()

    val mode by vm.mode.collectAsStateWithLifecycle()
    val chartMode by vm.chartModeModel.chartMode.collectAsStateWithLifecycle()
    val bookName by vm.bookName.collectAsStateWithLifecycle()

    var isExportDialogShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = stringResource(Res.string.overview_title, bookName.orEmpty()),
            menuActions = {
                MenuAction(
                    imageVector = when (mode) {
                        OverviewMode.AllRecords -> vectorResource(Res.drawable.ic_format_list_numbered)
                        OverviewMode.GroupByCategories -> chartMode.icon
                    },
                    description = stringResource(Res.string.overview_details_title),
                    onClick = vm::toggleMode,
                    modifier = Modifier.onPlaced {
                        vm.highlightModeButton(
                            BubbleDest.OverviewMode(
                                size = it.size,
                                offset = it::positionInRoot
                            )
                        )
                    }
                )

                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_file_download),
                    description = stringResource(Res.string.export_cta),
                    onClick = { isExportDialogShown = true },
                    modifier = Modifier.onPlaced {
                        vm.highlightExportButton(
                            BubbleDest.OverviewExport(
                                size = it.size,
                                offset = it::positionInRoot
                            )
                        )
                    }
                )
            }
        )

        AdaptiveScreen(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .weight(1F),
            regularContent = {
                OverviewContent(
                    state = vm.state,
                    navController = navController,
                )
            },
            wideContent = {
                OverviewContentWide(
                    state = vm.state,
                    navController = navController,
                )
            }
        )
    }

    val fromDate by vm.timeModel.fromDate.collectAsStateWithLifecycle()
    val untilDate by vm.timeModel.untilDate.collectAsStateWithLifecycle()

    if (isExportDialogShown) {
        ConfirmDialog(
            message = stringResource(
                Res.string.export_csv_confirmation,
                fromDate.shortFormatted,
                untilDate.shortFormatted
            ),
            onConfirm = {
                if (shouldRequestStoragePermission) {
                    scope.launch {
                        try {
                            controller.providePermission(Permission.WRITE_STORAGE)
                            vm.exportToCsv()
                        } catch (_: DeniedException) {
                            vm.showWriteFilePermissionHint()
                        } catch (_: DeniedAlwaysException) {
                            vm.showWriteFilePermissionHint()
                        }
                    }
                } else {
                    vm.exportToCsv()
                }
                isExportDialogShown = false
            },
            onDismiss = { isExportDialogShown = false }
        )
    }
}

enum class OverviewUiType {
    Header, Record, Group, PieChart, ZeroCase, Loader
}
