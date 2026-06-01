package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.runtime.Immutable
import com.kevlina.budgetplus.core.common.Currency

@Immutable
data class CurrencyState(
    val currency: Currency,
    val isSelected: Boolean,
    val isPinned: Boolean,
)