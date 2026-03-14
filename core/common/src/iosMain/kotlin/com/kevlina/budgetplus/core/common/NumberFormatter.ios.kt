package com.kevlina.budgetplus.core.common

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfUp

private val ratioFormat by lazy {
    NSNumberFormatter().apply {
        locale = NSLocale.appLocale
        minimumFractionDigits = 0u
        maximumFractionDigits = 1u
        roundingMode = NSNumberFormatterRoundHalfUp
        numberStyle = NSNumberFormatterDecimalStyle
    }
}

private val priceFormat by lazy {
    NSNumberFormatter().apply {
        locale = NSLocale.appLocale
        minimumFractionDigits = 0u
        maximumFractionDigits = 2u
        roundingMode = NSNumberFormatterRoundHalfUp
        numberStyle = NSNumberFormatterDecimalStyle
    }
}

private val priceFormatWithoutSeparator by lazy {
    NSNumberFormatter().apply {
        locale = NSLocale.appLocale
        minimumFractionDigits = 0u
        maximumFractionDigits = 2u
        roundingMode = NSNumberFormatterRoundHalfUp
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
    }
}

actual val Double.roundUpRatioText: String
    get() = ratioFormat.stringFromNumber(NSNumber(this)) ?: this.toString()

actual fun CharSequence.parseToPrice(): Double {
    return priceFormat.numberFromString(toString())?.doubleValue ?: 0.0
}

actual val Double.plainPriceString: String
    get() = priceFormatWithoutSeparator.stringFromNumber(NSNumber(this)) ?: this.toString()