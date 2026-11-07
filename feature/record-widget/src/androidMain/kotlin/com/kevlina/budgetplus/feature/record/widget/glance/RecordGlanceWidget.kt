package com.kevlina.budgetplus.feature.record.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.feature.record.widget.R
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetConfig
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetConfigStore
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetDeeplink
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetDimens
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetGraph
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetItem
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetSamples
import com.kevlina.budgetplus.feature.record.widget.config.RecordWidgetConfigActivity
import com.kevlina.budgetplus.feature.record.widget.isRecordWidgetDynamicColorSupported
import dev.zacsweers.metro.Inject

/**
 * The record widget on the home screen: the book name, followed by the frequent items. Tapping
 * an item opens the record screen with the item prefilled.
 */
class RecordGlanceWidget : GlanceAppWidget() {

    @Inject private lateinit var configStore: RecordWidgetConfigStore
    @Inject private lateinit var themeManager: ThemeManager

    // The exact size is needed to decide whether the name and the pills fit in the same row.
    override val sizeMode: SizeMode = SizeMode.Exact

    // The default 3x2 size, the provider's min size is too small to show the sample items.
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        inject(context)

        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id).toString()

        provideContent {
            val configs by configStore.configs.collectAsState(initial = null)
            val themeColors by themeManager.themeColors.collectAsState()

            // Render nothing until the configs are loaded, to avoid flashing the setup state.
            val loadedConfigs = configs ?: return@provideContent
            val config = loadedConfigs.firstOrNull { it.id == widgetId }
            RecordWidgetContent(
                widgetId = widgetId,
                config = config,
                colors = if (config?.useDynamicColors == true && isRecordWidgetDynamicColorSupported) {
                    WidgetColors.dynamic()
                } else {
                    WidgetColors.fromTheme(themeColors)
                },
            )
        }
    }

    /**
     * Android 15+: the generated preview in the widget picker, the sample items in the app's
     * current theme. It's published by [com.kevlina.budgetplus.feature.record.widget.RecordWidgetPlatform.updatePreview].
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        inject(context)
        val config = context.sampleRecordWidgetConfig()

        provideContent {
            val themeColors by themeManager.themeColors.collectAsState()
            RecordWidgetContent(
                widgetId = config.id,
                config = config,
                colors = WidgetColors.fromTheme(themeColors),
                isInteractive = false,
            )
        }
    }

    private fun inject(context: Context) {
        context.resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)
    }
}

/**
 * The localized sample widget, the categories match the app's default categories.
 */
internal fun Context.sampleRecordWidgetConfig(): RecordWidgetConfig {
    val food = getString(R.string.record_widget_sample_category_food)
    return RecordWidgetSamples.config.copy(
        bookName = getString(R.string.record_widget_sample_book),
        items = listOf(
            RecordWidgetItem(RecordType.Expense, food, getString(R.string.record_widget_sample_coffee)),
            RecordWidgetItem(RecordType.Expense, food, getString(R.string.record_widget_sample_lunch)),
            RecordWidgetItem(
                type = RecordType.Expense,
                category = getString(R.string.record_widget_sample_category_transport),
                name = getString(R.string.record_widget_sample_commute),
            ),
            RecordWidgetItem(
                type = RecordType.Expense,
                category = getString(R.string.record_widget_sample_category_daily),
                name = getString(R.string.record_widget_sample_groceries),
            ),
            RecordWidgetItem(
                type = RecordType.Income,
                category = getString(R.string.record_widget_sample_category_salary),
                name = getString(R.string.record_widget_sample_salary),
            ),
        ),
    )
}

// The layout is shared with the in-app preview.
private val HorizontalPadding = RecordWidgetDimens.HorizontalPadding
private val NameFontSize = RecordWidgetDimens.NameFontSize
private val PillFontSize = RecordWidgetDimens.PillFontSize
private val PillHorizontalPadding = RecordWidgetDimens.PillHorizontalPadding
private val NamePillsSpacing = RecordWidgetDimens.NamePillsSpacing
private val PillsSpacing = RecordWidgetDimens.PillsSpacing

/**
 * @param isInteractive False for the previews, which can't be tapped.
 */
@Composable
private fun RecordWidgetContent(
    widgetId: String,
    config: RecordWidgetConfig?,
    colors: WidgetColors,
    isInteractive: Boolean = true,
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(RecordWidgetDimens.CornerRadius)
    ) {
        if (config == null) {
            SetupContent(widgetId = widgetId, colors = colors)
            return@Column
        }

        Text(
            text = config.bookName,
            maxLines = 1,
            style = TextStyle(
                color = colors.headerText,
                fontSize = RecordWidgetDimens.HeaderFontSize,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.headerBackground)
                .padding(horizontal = HorizontalPadding, vertical = RecordWidgetDimens.HeaderVerticalPadding)
                // Open the app on the widget's book, without prefill.
                .thenIf(isInteractive) {
                    clickable(context.openDeeplinkAction(RecordWidgetDeeplink.buildBookLink(config.bookId)))
                }
        )

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(config.items) { index, item ->
                ItemRow(
                    item = item,
                    colors = colors,
                    isLast = index == config.items.lastIndex,
                    onClick = if (isInteractive) {
                        context.openDeeplinkAction(RecordWidgetDeeplink.build(config.bookId, item))
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: RecordWidgetItem,
    colors: WidgetColors,
    isLast: Boolean,
    onClick: Action?,
) {
    val context = LocalContext.current
    val pills = buildList {
        add(item.category)
        if (item.type == RecordType.Income) add(context.getString(R.string.record_widget_income))
    }
    val isSingleRow = context.fitsInSingleRow(name = item.name, pills = pills, widgetSize = LocalSize.current)

    Column(modifier = GlanceModifier.fillMaxWidth().thenIf(onClick != null) { clickable(onClick!!) }) {
        if (isSingleRow) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth().padding(
                    horizontal = HorizontalPadding,
                    vertical = RecordWidgetDimens.SingleRowVerticalPadding,
                )
            ) {
                ItemName(name = item.name, colors = colors)
                Spacer(modifier = GlanceModifier.width(NamePillsSpacing))
                Pills(pills = pills, colors = colors)
            }
        } else {
            // The name is too long, move the pills to the 2nd row.
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(
                    horizontal = HorizontalPadding,
                    vertical = RecordWidgetDimens.TwoRowsVerticalPadding,
                )
            ) {
                ItemName(name = item.name, colors = colors)
                Spacer(modifier = GlanceModifier.height(RecordWidgetDimens.TwoRowsSpacing))
                Pills(pills = pills, colors = colors)
            }
        }

        if (!isLast) {
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(RecordWidgetDimens.DividerHeight)
                    .background(colors.divider)
            )
        }
    }
}

@Composable
private fun ItemName(name: String, colors: WidgetColors) {
    Text(
        text = name,
        maxLines = 1,
        style = TextStyle(
            color = colors.itemText,
            fontSize = NameFontSize,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@Composable
private fun Pills(pills: List<String>, colors: WidgetColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        pills.forEachIndexed { index, pill ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(PillsSpacing))
            Box(
                modifier = GlanceModifier
                    .background(colors.pillBackground)
                    .cornerRadius(RecordWidgetDimens.PillCornerRadius)
                    .padding(horizontal = PillHorizontalPadding, vertical = RecordWidgetDimens.PillVerticalPadding)
            ) {
                Text(
                    text = pill,
                    maxLines = 1,
                    style = TextStyle(color = colors.pillText, fontSize = PillFontSize),
                )
            }
        }
    }
}

/**
 * The config could be missing, e.g. the app data was cleared. Let the user configure it again.
 */
@Composable
private fun SetupContent(widgetId: String, colors: WidgetColors) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, RecordWidgetConfigActivity::class.java)
                        .putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            widgetId.toIntOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID,
                        )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            )
    ) {
        Text(
            text = context.getString(R.string.record_widget_setup),
            style = TextStyle(
                color = colors.itemText,
                fontSize = NameFontSize,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Glance can't measure the layout, so measure the texts with [Paint] to decide whether the name
 * and the pills fit in the same row.
 */
private fun Context.fitsInSingleRow(name: String, pills: List<String>, widgetSize: DpSize): Boolean {
    val metrics = resources.displayMetrics
    fun Float.dpToPx() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, metrics)
    fun TextUnit.spToPx() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, metrics)

    // Measure with bold to slightly overestimate the medium weight name, rather than overflow.
    val namePaint = Paint().apply {
        textSize = NameFontSize.spToPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val pillPaint = Paint().apply { textSize = PillFontSize.spToPx() }

    val pillsWidth = pills.sumOf { pill ->
        (pillPaint.measureText(pill) + (PillHorizontalPadding.value * 2).dpToPx()).toDouble()
    } + PillsSpacing.value.dpToPx() * (pills.size - 1)
    val requiredWidth = namePaint.measureText(name) + NamePillsSpacing.value.dpToPx() + pillsWidth
    val availableWidth = (widgetSize.width.value - HorizontalPadding.value * 2).dpToPx()
    return requiredWidth <= availableWidth
}

private fun Context.openDeeplinkAction(deeplink: String): Action {
    val intent = Intent(Intent.ACTION_VIEW, deeplink.toUri())
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return actionStartActivity(intent)
}

/**
 * The color roles of the widget, either from the app's theme or the system dynamic colors.
 */
internal data class WidgetColors(
    val background: ColorProvider,
    val headerBackground: ColorProvider,
    val headerText: ColorProvider,
    val itemText: ColorProvider,
    val pillBackground: ColorProvider,
    val pillText: ColorProvider,
    val divider: ColorProvider,
) {
    companion object {

        fun fromTheme(colors: ThemeColors) = WidgetColors(
            background = colors.light.toColorProvider(),
            headerBackground = colors.dark.toColorProvider(),
            headerText = colors.light.toColorProvider(),
            itemText = colors.dark.toColorProvider(),
            pillBackground = colors.primary.toColorProvider(),
            pillText = colors.light.toColorProvider(),
            divider = colors.lightBg.toColorProvider(),
        )

        /**
         * Material You. The default Glance colors are backed by the system dynamic color resources
         * on Android 12+, so the launcher resolves them when rendering. The widget follows the
         * wallpaper and dark mode changes without the app running.
         */
        @Composable
        fun dynamic(): WidgetColors {
            val colors = GlanceTheme.colors
            return WidgetColors(
                background = colors.widgetBackground,
                headerBackground = colors.primary,
                headerText = colors.onPrimary,
                itemText = colors.onSurface,
                pillBackground = colors.secondaryContainer,
                pillText = colors.onSecondaryContainer,
                divider = colors.surfaceVariant,
            )
        }
    }
}

private inline fun GlanceModifier.thenIf(
    condition: Boolean,
    modifier: GlanceModifier.() -> GlanceModifier,
): GlanceModifier = if (condition) modifier() else this

// The theme colors are the same in light and dark mode.
private fun Color.toColorProvider(): ColorProvider = ColorProvider(day = this, night = this)

private val previewConfig = RecordWidgetSamples.config

// The long name moves the pills to the 2nd row.
private val longNameConfig = RecordWidgetSamples.config.copy(
    items = listOf(
        RecordWidgetItem(type = RecordType.Expense, category = "Rent", name = "36 Cassia Crescent Apartment"),
    ) + RecordWidgetSamples.items.take(3),
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun RecordWidgetContent_Preview() {
    RecordWidgetContent(widgetId = "preview", config = previewConfig, colors = WidgetColors.fromTheme(ThemeColors.MilkTea))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun RecordWidgetContentNarrow_Preview() {
    RecordWidgetContent(widgetId = "preview", config = longNameConfig, colors = WidgetColors.fromTheme(ThemeColors.Dusk))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun RecordWidgetContentDynamic_Preview() {
    RecordWidgetContent(widgetId = "preview", config = previewConfig, colors = WidgetColors.dynamic())
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 120)
@Composable
private fun RecordWidgetSetup_Preview() {
    RecordWidgetContent(widgetId = "preview", config = null, colors = WidgetColors.fromTheme(ThemeColors.MilkTea))
}
