package com.kevlina.budgetplus.core.ads

/**
 * The moments that can show an interstitial ad. [key] is the value used in the
 * `ads_interstitial_placements` Remote Config, see [InterstitialAdsConfig].
 */
enum class InterstitialPlacement(val key: String) {

    /** The user switches to the History tab from outside of it. */
    EnterHistory("enter_history"),

    /** The user goes back to the overview from the category details or the search. */
    LeaveDetail("leave_detail"),

    /** The user just saved a record, on every 7th record. Enabled by default, the legacy behavior. */
    RecordSaved("record_saved"),

    /**
     * The user exports the records to CSV, the export starts after the ad is closed.
     * Enabled by default, the legacy behavior.
     */
    CsvExport("csv_export");

    companion object {
        fun fromKey(key: String): InterstitialPlacement? = entries.firstOrNull { it.key == key }
    }
}
