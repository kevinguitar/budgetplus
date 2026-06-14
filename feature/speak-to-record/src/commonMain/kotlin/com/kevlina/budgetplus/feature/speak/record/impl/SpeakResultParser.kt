package com.kevlina.budgetplus.feature.speak.record.impl

import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordStatus
import dev.zacsweers.metro.Inject

@Inject
class SpeakResultParser(
    private val tracker: Tracker,
) {

    fun parse(text: String?): SpeakToRecordStatus {
        if (text.isNullOrBlank()) {
            return SpeakToRecordStatus.NoResult
        }

        val matchResult = namePriceRegex.find(text)
        tracker.logEvent("speak_to_record_success")
        return if (matchResult != null) {
            val name = matchResult.groupValues[1].trim()
            val price = matchResult.groupValues[2].toDoubleOrNull()
            Logger.d("SpeechRecognizer: Parsed $name, $price")
            SpeakToRecordStatus.Success(name, price)
        } else {
            // No price found, use text as name and ignore price
            SpeakToRecordStatus.Success(text, null)
        }
    }
}

/**
 * A regular expression used to extract the name and price from a string.
 *
 * This regex is designed to handle strings in the format:
 *  - "Name Price [Optional Unit]"
 *
 * It captures two groups:
 *  - Group 1: The name of the item (.*?)
 *  - Group 2: The price of the item (\\d+(?:\\.\\d+)?)
 *
 * The optional unit (e.g., "USD", "kg") is matched but not captured.
 *
 * Examples:
 *  - "Apple 1.25 USD" -> Group 1: "Apple", Group 2: "1.25"
 *  - "Banana 0.75" -> Group 1: "Banana", Group 2: "0.75"
 *  - "Orange Juice 2.50 per liter" -> Group 1: "Orange Juice", Group 2: "2.50"
 */
private val namePriceRegex = Regex("(.*?)(\\d+(?:\\.\\d+)?)(?:\\s?\\w+)?$")
