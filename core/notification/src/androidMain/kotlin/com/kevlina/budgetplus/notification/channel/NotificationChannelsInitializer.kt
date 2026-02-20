package com.kevlina.budgetplus.notification.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.notification_channel_general
import budgetplus.core.common.generated.resources.notification_channel_general_desc
import budgetplus.core.common.generated.resources.notification_channel_new_member
import budgetplus.core.common.generated.resources.notification_channel_new_member_desc
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.notification.NotificationConstants.CHANNEL_GENERAL
import com.kevlina.budgetplus.notification.NotificationConstants.CHANNEL_NEW_MEMBER
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ContributesIntoSet(AppScope::class)
class NotificationChannelsInitializer(
    private val context: Context,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    override fun onAppStart() {
        appScope.launch { initNotificationChannels() }
    }

    private suspend fun initNotificationChannels() {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val generalChannel = createChannel(
            channelId = CHANNEL_GENERAL,
            name = getString(Res.string.notification_channel_general),
            descriptionText = getString(Res.string.notification_channel_general_desc)
        )

        val newMemberChannel = createChannel(
            channelId = CHANNEL_NEW_MEMBER,
            name = getString(Res.string.notification_channel_new_member),
            descriptionText = getString(Res.string.notification_channel_new_member_desc)
        )

        notificationManager.createNotificationChannel(generalChannel)
        notificationManager.createNotificationChannel(newMemberChannel)
    }

    private fun createChannel(
        channelId: String,
        name: String,
        descriptionText: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    ): NotificationChannel {
        return NotificationChannel(channelId, name, importance)
            .apply { description = descriptionText }
    }
}