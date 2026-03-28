package com.kevlina.budgetplus.feature.insider.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.feature.insider.generated.resources.Res
import budgetplus.feature.insider.generated.resources.insider_language_en
import budgetplus.feature.insider.generated.resources.insider_language_ja
import budgetplus.feature.insider.generated.resources.insider_language_ko
import budgetplus.feature.insider.generated.resources.insider_language_zh_cn
import budgetplus.feature.insider.generated.resources.insider_language_zh_tw
import budgetplus.feature.insider.generated.resources.insider_user_created_on
import budgetplus.feature.insider.generated.resources.insider_user_last_active_on
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kevlina.budgetplus.core.common.millisToDateTime
import com.kevlina.budgetplus.core.common.shortFormatted
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Composable
internal fun UserCard(
    user: User,
) {

    val createOn = remember(user) {
        user.createdOn?.millisToDateTime()?.shortFormatted
    }

    val lastActiveOn = remember(user) {
        user.lastActiveOn?.millisToDateTime()?.shortFormatted
    }

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clip(AppTheme.cardShape)
            .background(LocalAppColors.current.lightBg)
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            AsyncImage(
                model = ImageRequest.Builder(LocalActivity.current!!)
                    .data(user.photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = user.name.orEmpty(),
                    fontSize = FontSize.SemiLarge,
                    fontWeight = FontWeight.SemiBold
                )

                user.language?.let { language ->
                    Spacer(modifier = Modifier
                        .size(2.dp)
                        .clip(CircleShape)
                        .background(LocalAppColors.current.dark)
                    )

                    Text(text = stringResource(when (language) {
                        "zh-tw" -> Res.string.insider_language_zh_tw
                        "zh-cn" -> Res.string.insider_language_zh_cn
                        "ja" -> Res.string.insider_language_ja
                        "ko" -> Res.string.insider_language_ko
                        else -> Res.string.insider_language_en
                    }))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (createOn != null) {
            Text(
                text = stringResource(Res.string.insider_user_created_on, createOn)
            )
        }

        if (lastActiveOn != null && lastActiveOn != createOn) {
            Text(
                text = stringResource(Res.string.insider_user_last_active_on, lastActiveOn)
            )
        }
    }
}

@Preview
@Composable
private fun UserCard_Preview() = AppTheme {
    UserCard(user = User(
        name = "Kevin Chiu",
        createdOn = Clock.System.now().minus(1.days).toEpochMilliseconds(),
        lastActiveOn = Clock.System.now().toEpochMilliseconds(),
        language = "zh-tw"
    ))
}