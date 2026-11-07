package com.kevlina.budgetplus.feature.push.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import budgetplus.feature.push_notifications.generated.resources.Res
import budgetplus.feature.push_notifications.generated.resources.push_notif_title
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.safeHorizontal
import com.kevlina.budgetplus.feature.push.notifications.ui.PushNotificationsContent
import org.jetbrains.compose.resources.stringResource

@Composable
fun PushNotificationsScreen(
    navigateUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = stringResource(Res.string.push_notif_title),
            navigateUp = navigateUp,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
                .windowInsetsPadding(WindowInsets.safeHorizontal)
        ) {
            PushNotificationsContent(
                navigateUp = navigateUp
            )
        }
    }
}