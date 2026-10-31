package com.kevlina.budgetplus.feature.record.widget

/**
 * Platform specific operations of the record widget.
 */
interface RecordWidgetPlatform {

    /**
     * Add the widget to the home screen with the given config, this is used when the flow starts
     * from the app's settings.
     */
    suspend fun addWidget(config: RecordWidgetConfig): AddWidgetResult

    /**
     * Re-render all the widgets, e.g. after the configs or the theme changed.
     */
    suspend fun refreshWidgets()
}

enum class AddWidgetResult {
    /** Android: the launcher is showing its own confirmation to pin the widget. */
    PinRequested,

    /** Android: the launcher doesn't support pinning, the user has to add it manually. */
    PinUnsupported,

    /** iOS: the preset is saved, the user has to add the widget from the home screen. */
    PresetSaved,
}
