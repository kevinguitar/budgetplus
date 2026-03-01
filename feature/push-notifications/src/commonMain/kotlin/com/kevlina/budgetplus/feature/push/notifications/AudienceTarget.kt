package com.kevlina.budgetplus.feature.push.notifications

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.push_notif_target_audience_all
import budgetplus.core.common.generated.resources.push_notif_target_audience_free_users
import budgetplus.core.common.generated.resources.push_notif_target_audience_paid_users
import org.jetbrains.compose.resources.StringResource

enum class AudienceTarget {
    All, FreeUsers, PaidUsers;

    fun toStringRes(): StringResource {
        return when (this) {
            All -> Res.string.push_notif_target_audience_all
            FreeUsers -> Res.string.push_notif_target_audience_free_users
            PaidUsers -> Res.string.push_notif_target_audience_paid_users
        }
    }
}