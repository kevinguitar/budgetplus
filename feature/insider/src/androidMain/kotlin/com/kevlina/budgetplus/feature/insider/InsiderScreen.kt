package com.kevlina.budgetplus.feature.insider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import budgetplus.core.common.generated.resources.cta_add
import budgetplus.core.common.generated.resources.ic_notification_add
import budgetplus.feature.insider.generated.resources.Res
import budgetplus.feature.insider.generated.resources.insider_title
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.feature.insider.ui.InsiderContent
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import budgetplus.core.common.generated.resources.Res as CommonRes

@Composable
fun InsiderScreen(
    openPushNotifications: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = stringResource(Res.string.insider_title),
            menuActions = {
                MenuAction(
                    imageVector = vectorResource(CommonRes.drawable.ic_notification_add),
                    description = stringResource(CommonRes.string.cta_add),
                    onClick = openPushNotifications,
                )
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            InsiderContent()
        }
    }
}