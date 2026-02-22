package com.kevlina.budgetplus.core.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.stringPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_bar_chart
import budgetplus.core.common.generated.resources.ic_pie_chart
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.vectorResource

@Serializable
enum class ChartMode {
    BarChart, PieChart
}

val ChartMode.icon: ImageVector
    @Composable
    get() = when (this) {
        ChartMode.BarChart -> vectorResource(Res.drawable.ic_bar_chart)
        ChartMode.PieChart -> vectorResource(Res.drawable.ic_pie_chart)
    }

@SingleIn(AppScope::class)
@Inject
class ChartModeViewModel(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val preference: Preference,
    private val tracker: Tracker,
) {
    private val chartModeKey = stringPreferencesKey("chartModeCache")

    val chartMode: StateFlow<ChartMode> = preference.of(
        key = chartModeKey, serializer = ChartMode.serializer(), default = ChartMode.BarChart, scope = appScope
    )

    val chartModeAnalyticsName: String
        get() = when (chartMode.value) {
            ChartMode.BarChart -> "bar_chart"
            ChartMode.PieChart -> "pie_chart"
        }

    fun setChartMode(mode: ChartMode) {
        appScope.launch {
            preference.update(chartModeKey, ChartMode.serializer(), mode)
        }
        tracker.logEvent(
            event = "chart_mode_changed",
            params = mapOf("chart_mode" to chartModeAnalyticsName)
        )
    }
}