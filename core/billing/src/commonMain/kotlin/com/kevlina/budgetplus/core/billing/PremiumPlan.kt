package com.kevlina.budgetplus.core.billing

enum class PremiumPlan {
    Monthly, Annual, Lifetime
}

data class Pricing(
    val discountedPrice: String?,
    val formattedPrice: String,
    val freeTrialDays: Int?,
)