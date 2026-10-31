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
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetGraph
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetItem
import com.kevlina.budgetplus.feature.record.widget.config.RecordWidgetConfigActivity
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)

        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id).toString()

        provideContent {
            val configs by configStore.configs.collectAsState(initial = null)
            val themeColors by themeManager.themeColors.collectAsState()

            // Render nothing until the configs are loaded, to avoid flashing the setup state.
            val loadedConfigs = configs ?: return@provideContent
            RecordWidgetContent(
                widgetId = widgetId,
                config = loadedConfigs.firstOrNull { it.id == widgetId },
                colors = themeColors,
            )
        }
    }
}

private val HorizontalPadding = 16.dp
private val NameFontSize = 15.sp
private val PillFontSize = 12.sp
private val PillHorizontalPadding = 8.dp
private val NamePillsSpacing = 8.dp
private val PillsSpacing = 6.dp

@Composable
private fun RecordWidgetContent(
    widgetId: String,
    config: RecordWidgetConfig?,
    colors: ThemeColors,
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.light)
            .cornerRadius(16.dp)
    ) {
        if (config == null) {
            SetupContent(widgetId = widgetId, colors = colors)
            return@Column
        }

        Text(
            text = config.bookName,
            maxLines = 1,
            style = TextStyle(
                color = colors.light.toColorProvider(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.dark)
                .padding(horizontal = HorizontalPadding, vertical = 10.dp)
        )

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(config.items) { index, item ->
                ItemRow(
                    item = item,
                    colors = colors,
                    isLast = index == config.items.lastIndex,
                    onClick = context.openDeeplinkAction(RecordWidgetDeeplink.build(config.bookId, item)),
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: RecordWidgetItem,
    colors: ThemeColors,
    isLast: Boolean,
    onClick: Action,
) {
    val context = LocalContext.current
    val pills = buildList {
        add(item.category)
        if (item.type == RecordType.Income) add(context.getString(R.string.record_widget_income))
    }
    val isSingleRow = context.fitsInSingleRow(name = item.name, pills = pills, widgetSize = LocalSize.current)

    Column(modifier = GlanceModifier.fillMaxWidth().clickable(onClick)) {
        if (isSingleRow) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 10.dp)
            ) {
                ItemName(name = item.name, colors = colors)
                Spacer(modifier = GlanceModifier.width(NamePillsSpacing))
                Pills(pills = pills, colors = colors)
            }
        } else {
            // The name is too long, move the pills to the 2nd row.
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 8.dp)
            ) {
                ItemName(name = item.name, colors = colors)
                Spacer(modifier = GlanceModifier.height(4.dp))
                Pills(pills = pills, colors = colors)
            }
        }

        if (!isLast) {
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.lightBg)
            )
        }
    }
}

@Composable
private fun ItemName(name: String, colors: ThemeColors) {
    Text(
        text = name,
        maxLines = 1,
        style = TextStyle(
            color = colors.dark.toColorProvider(),
            fontSize = NameFontSize,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@Composable
private fun Pills(pills: List<String>, colors: ThemeColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        pills.forEachIndexed { index, pill ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(PillsSpacing))
            Box(
                modifier = GlanceModifier
                    .background(colors.primary)
                    .cornerRadius(8.dp)
                    .padding(horizontal = PillHorizontalPadding, vertical = 1.dp)
            ) {
                Text(
                    text = pill,
                    maxLines = 1,
                    style = TextStyle(color = colors.light.toColorProvider(), fontSize = PillFontSize),
                )
            }
        }
    }
}

/**
 * The config could be missing, e.g. the app data was cleared. Let the user configure it again.
 */
@Composable
private fun SetupContent(widgetId: String, colors: ThemeColors) {
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
                color = colors.dark.toColorProvider(),
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

// The theme colors are the same in light and dark mode.
private fun Color.toColorProvider(): ColorProvider = ColorProvider(day = this, night = this)

private val previewConfig = RecordWidgetConfig(
    id = "preview",
    bookId = "book",
    bookName = "Family Book",
    items = listOf(
        RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner"),
        RecordWidgetItem(type = RecordType.Expense, category = "Rent", name = "36 Cassia Crescent Apartment"),
        RecordWidgetItem(type = RecordType.Expense, category = "Transport", name = "Transport"),
        RecordWidgetItem(type = RecordType.Income, category = "Salary", name = "Salary"),
    ),
    createdOn = 0,
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun RecordWidgetContent_Preview() {
    RecordWidgetContent(widgetId = "preview", config = previewConfig, colors = ThemeColors.MilkTea)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun RecordWidgetContentNarrow_Preview() {
    RecordWidgetContent(widgetId = "preview", config = previewConfig, colors = ThemeColors.Dusk)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 120)
@Composable
private fun RecordWidgetSetup_Preview() {
    RecordWidgetContent(widgetId = "preview", config = null, colors = ThemeColors.MilkTea)
}
