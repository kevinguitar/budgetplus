package com.kevlina.budgetplus.core.ui.bubble

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.bubble_colors_sharing
import budgetplus.core.common.generated.resources.bubble_edit_category
import budgetplus.core.common.generated.resources.bubble_invite
import budgetplus.core.common.generated.resources.bubble_overview_export
import budgetplus.core.common.generated.resources.bubble_overview_mode
import budgetplus.core.common.generated.resources.bubble_overview_pie_chart
import budgetplus.core.common.generated.resources.bubble_overview_record_tap_hint
import budgetplus.core.common.generated.resources.bubble_records_sorting
import budgetplus.core.common.generated.resources.bubble_save_category
import budgetplus.core.common.generated.resources.bubble_scroll_to_select_currency
import budgetplus.core.common.generated.resources.bubble_speak_to_record
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource

interface BubbleRepo {
    val bubble: StateFlow<BubbleDest?>

    suspend fun addBubbleToQueue(dest: BubbleDest)
    fun popBubble()
}

sealed class BubbleDest {

    abstract val key: String
    abstract val size: IntSize

    /**
     * Lazily resolve offset to make sure UI is placed steadily on the screen.
     * Note that this invocation could throw a [IllegalStateException] due to node not being attached.
     */
    abstract val offset: () -> Offset
    abstract val shape: BubbleShape

    abstract val textRes: StringResource
    abstract val textDirection: BubbleTextDirection

    data class Invite(
        override val key: String = "isInviteBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_invite,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

    data class SpeakToRecord(
        override val key: String = "isSpeakToRecordBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_speak_to_record,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.TopEnd,
    ) : BubbleDest()

    data class ScrollToSelectCurrency(
        override val key: String = "isScrollToSelectCurrencyBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape,
        override val textRes: StringResource = Res.string.bubble_scroll_to_select_currency,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomCenter,
    ) : BubbleDest()

    data class EditCategoriesHint(
        override val key: String = "isEditHintBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape,
        override val textRes: StringResource = Res.string.bubble_edit_category,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomCenter,
    ) : BubbleDest()

    data class SaveCategories(
        override val key: String = "isSaveBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_save_category,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

    data class OverviewMode(
        override val key: String = "isModeBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_overview_mode,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

    data class OverviewExport(
        override val key: String = "isExportBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_overview_export,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

    data class OverviewRecordTapHint(
        override val key: String = "isTapHintBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape,
        override val textRes: StringResource = Res.string.bubble_overview_record_tap_hint,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomCenter,
    ) : BubbleDest()

    data class OverviewPieChart(
        override val key: String = "isPieChartBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_overview_pie_chart,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.TopCenter,
    ) : BubbleDest()

    data class RecordsSorting(
        override val key: String = "isSortingBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_records_sorting,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

    data class ColorsSharing(
        override val key: String = "isShareColorsBubbleShown",
        override val size: IntSize,
        override val offset: () -> Offset,
        override val shape: BubbleShape = BubbleShape.Circle,
        override val textRes: StringResource = Res.string.bubble_colors_sharing,
        override val textDirection: BubbleTextDirection = BubbleTextDirection.BottomEnd,
    ) : BubbleDest()

}

enum class BubbleTextDirection {
    TopStart, TopEnd, TopCenter,
    BottomStart, BottomEnd, BottomCenter
}

sealed class BubbleShape {
    data object Circle : BubbleShape()
    data class RoundedRect(val corner: Float) : BubbleShape()
}