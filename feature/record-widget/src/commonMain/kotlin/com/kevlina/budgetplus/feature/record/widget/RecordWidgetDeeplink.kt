package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK_CUSTOM
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_PATH
import com.kevlina.budgetplus.core.data.RecordPrefill

/**
 * Builds and parses the deeplink of tapping an item on the record widget, e.g.
 * `budgetplus://record?source=widget&bookId=abc&type=Expense&category=Food&name=Dinner`
 *
 * The link is self-contained so it still works when the widget config is gone. Note that the
 * iOS widget extension builds the same link in Swift, keep `RecordWidgetDeeplink.swift` in sync.
 */
object RecordWidgetDeeplink {

    const val SOURCE_WIDGET = "widget"

    private const val KEY_SOURCE = "source"
    private const val KEY_BOOK_ID = "bookId"
    private const val KEY_TYPE = "type"
    private const val KEY_CATEGORY = "category"
    private const val KEY_NAME = "name"

    fun build(bookId: String, item: RecordWidgetItem): String {
        val query = listOf(
            KEY_SOURCE to SOURCE_WIDGET,
            KEY_BOOK_ID to bookId,
            KEY_TYPE to item.type.name,
            KEY_CATEGORY to item.category,
            KEY_NAME to item.name,
        ).joinToString("&") { [key, value] -> "$key=${value.percentEncode()}" }
        return "$APP_DEEPLINK_CUSTOM$NAV_RECORD_PATH?$query"
    }

    /**
     * @param queries The raw (still encoded) query parameters of a record deeplink.
     * @return null if the link isn't from the widget or it's malformed.
     */
    fun parse(queries: Map<String, String>): RecordPrefill? {
        if (queries[KEY_SOURCE] != SOURCE_WIDGET) return null

        val bookId = queries[KEY_BOOK_ID]?.percentDecode()?.takeIf { it.isNotBlank() } ?: return null
        val type = queries[KEY_TYPE]?.let { raw -> RecordType.entries.firstOrNull { it.name == raw } }
            ?: return null
        val category = queries[KEY_CATEGORY]?.percentDecode()?.takeIf { it.isNotBlank() } ?: return null
        val name = queries[KEY_NAME]?.percentDecode()?.takeIf { it.isNotBlank() } ?: category

        return RecordPrefill(bookId = bookId, type = type, category = category, name = name)
    }
}

private const val UNRESERVED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * Percent-encode everything except the RFC 3986 unreserved characters, using UTF-8.
 */
internal fun String.percentEncode(): String = buildString {
    encodeToByteArray().forEach { byte ->
        val char = byte.toInt().toChar()
        if (byte >= 0 && char in UNRESERVED_CHARS) {
            append(char)
        } else {
            val value = byte.toInt() and 0xFF
            append('%')
            append(HEX_DIGITS[value shr 4])
            append(HEX_DIGITS[value and 0x0F])
        }
    }
}

/**
 * Decode the percent-encoded UTF-8 string. Unlike form encoding, `+` is kept as is.
 * Malformed escapes are kept as literal text.
 */
internal fun String.percentDecode(): String {
    val result = StringBuilder(length)
    val pendingBytes = ArrayList<Byte>()

    fun flushBytes() {
        if (pendingBytes.isEmpty()) return
        result.append(pendingBytes.toByteArray().decodeToString())
        pendingBytes.clear()
    }

    var index = 0
    while (index < length) {
        val hex = if (this[index] == '%' && index + 2 <= lastIndex) {
            substring(index + 1, index + 3)
                .takeIf { digits -> digits.all { it.uppercaseChar() in HEX_DIGITS } }
                ?.toInt(16)
        } else {
            null
        }

        if (hex != null) {
            pendingBytes.add(hex.toByte())
            index += 3
        } else {
            flushBytes()
            result.append(this[index])
            index += 1
        }
    }
    flushBytes()
    return result.toString()
}
