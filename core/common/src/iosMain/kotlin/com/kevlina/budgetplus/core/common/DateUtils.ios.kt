package com.kevlina.budgetplus.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone

actual val LocalDate.shortFormatted: String
    get() = format(NSDateFormatterShortStyle)

actual val LocalDate.mediumFormatted: String
    get() = format(NSDateFormatterMediumStyle)

actual val LocalDate.fullFormatted: String
    get() = format(NSDateFormatterFullStyle)

actual val LocalDateTime.shortFormatted: String
    get() {
        val formatter = NSDateFormatter()
        formatter.dateStyle = NSDateFormatterShortStyle
        formatter.timeStyle = NSDateFormatterShortStyle
        formatter.locale = NSLocale.appLocale

        val components = toNSDateComponents()
        components.hour = hour.toLong()
        components.minute = minute.toLong()
        components.second = second.toLong()
        components.nanosecond = nanosecond.toLong()
        components.timeZone = NSTimeZone.defaultTimeZone

        val date = NSCalendar.currentCalendar.dateFromComponents(components) ?: return ""
        return formatter.stringFromDate(date)
    }

private fun LocalDate.format(style: NSDateFormatterStyle): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = style
    formatter.timeStyle = NSDateFormatterNoStyle
    formatter.locale = NSLocale.appLocale

    val components = toNSDateComponents()
    val date = NSCalendar.currentCalendar.dateFromComponents(components) ?: return ""
    return formatter.stringFromDate(date)
}