package com.kevlina.budgetplus.core.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Purchase(
    val orderId: String? = null,
    val productId: String? = null,
    val userId: String? = null,
    val client: String? = null,
    val purchasedOn: Long = 0L,
)