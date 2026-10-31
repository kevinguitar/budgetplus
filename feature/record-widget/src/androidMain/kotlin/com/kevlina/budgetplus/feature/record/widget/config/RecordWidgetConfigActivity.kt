package com.kevlina.budgetplus.feature.record.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.cta_go
import budgetplus.core.common.generated.resources.record_widget_sign_in
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK_CUSTOM
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_PATH
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetGraph
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetViewModel
import com.kevlina.budgetplus.feature.record.widget.ui.RecordWidgetContent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Launched by the launcher when the user adds the widget from the widget picker, or reconfigures
 * an existing widget.
 */
class RecordWidgetConfigActivity : ComponentActivity() {

    @Inject private lateinit var themeManager: ThemeManager
    @Inject private lateinit var viewModelFactory: MetroViewModelFactory

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = viewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // The widget won't be added if the user backs out of the configuration.
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                val themeColors by themeManager.themeColors.collectAsStateWithLifecycle()
                AppTheme(themeColors) {
                    val vm = metroViewModel<RecordWidgetViewModel>()

                    if (!vm.isSignedIn || vm.book == null) {
                        MessageContent(
                            message = stringResource(Res.string.record_widget_sign_in, stringResource(Res.string.app_name)),
                            cta = stringResource(Res.string.cta_go),
                            onCtaClick = { openApp("$APP_DEEPLINK_CUSTOM$NAV_RECORD_PATH") },
                        )
                    } else {
                        RecordWidgetContent(
                            vm = vm,
                            navigateUp = ::finish,
                            onDone = {
                                lifecycleScope.launch {
                                    if (vm.saveConfigForWidget(widgetId = appWidgetId.toString()) != null) {
                                        setResult(RESULT_OK, resultIntent(appWidgetId))
                                        finish()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun resultIntent(appWidgetId: Int) = Intent()
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun openApp(deeplink: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, deeplink.toUri())
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}

@Composable
private fun MessageContent(
    message: String,
    cta: String,
    onCtaClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = message,
            fontSize = FontSize.SemiLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Button(onClick = onCtaClick) {
            Text(
                text = cta,
                color = LocalAppColors.current.light,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
