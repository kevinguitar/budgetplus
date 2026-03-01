package com.kevlina.budgetplus.feature.push.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.feature.push_notifications.generated.resources.Res
import budgetplus.feature.push_notifications.generated.resources.push_notif_language_ja
import budgetplus.feature.push_notifications.generated.resources.push_notif_language_zh_tw
import budgetplus.feature.push_notifications.generated.resources.push_notif_push_description
import budgetplus.feature.push_notifications.generated.resources.push_notif_push_title
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Switch
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LanguageBlock(
    textRes: StringResource,
    title: TextFieldState,
    description: TextFieldState,
    modifier: Modifier = Modifier,
    isOptional: Boolean = true,
    enabled: Boolean = true,
    onEnableUpdate: (Boolean) -> Unit = {},
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(textRes),
                fontSize = FontSize.Large,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1F)
            )

            if (isOptional) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnableUpdate,
                )
            }
        }

        if (enabled) {
            TextField(
                state = title,
                title = stringResource(Res.string.push_notif_push_title),
                singleLine = false,
                modifier = Modifier.height(80.dp)
            )

            TextField(
                state = description,
                title = stringResource(Res.string.push_notif_push_description),
                singleLine = false,
                modifier = Modifier.height(80.dp)
            )
        }
    }
}

@Preview
@Composable
private fun LanguageBlock_Preview() = AppTheme {
    LanguageBlock(
        textRes = Res.string.push_notif_language_zh_tw,
        title = rememberTextFieldState("推播標題\n第二行"),
        description = rememberTextFieldState("記得目標，存款不停歇！記帳確實，未來更悠遊！將花費化為理財力！GO~"),
        isOptional = false,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(horizontal = 16.dp)
    )
}

@Preview
@Composable
private fun LanguageBlockOptional_Preview() = AppTheme {
    LanguageBlock(
        textRes = Res.string.push_notif_language_ja,
        title = rememberTextFieldState("新しい月ですよ！"),
        description = rememberTextFieldState("月初めに支出の追跡を始めましょう"),
        isOptional = true,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(horizontal = 16.dp)
    )
}