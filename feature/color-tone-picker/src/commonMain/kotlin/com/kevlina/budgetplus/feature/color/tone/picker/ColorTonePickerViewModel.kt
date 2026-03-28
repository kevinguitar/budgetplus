package com.kevlina.budgetplus.feature.color.tone.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.color_tone_applied_from_link
import budgetplus.core.common.generated.resources.cta_share
import budgetplus.core.common.generated.resources.fallback_error_message
import budgetplus.core.common.generated.resources.menu_share_colors
import com.kevlina.budgetplus.core.common.ShareHelper
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.theme.ColorTone
import com.kevlina.budgetplus.core.theme.ThemeColorSemantic
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.ViewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey
@ContributesIntoMap(ViewModelScope::class)
class ColorTonePickerViewModel(
    val navController: NavController<BookDest>,
    authManager: AuthManager,
    private val themeManager: ThemeManager,
    private val bubbleRepo: BubbleRepo,
    private val snackbarSender: SnackbarSender,
    private val shareHelper: ShareHelper,
    private val tracker: Tracker,
) : ViewModel() {

    val isPremium = authManager.isPremium

    val previewColors = themeManager.previewColors
    var previousProcessedLink: String? = null

    private val currentColorTone get() = themeManager.colorTone.value

    val selectedColorTone: StateFlow<ColorTone>
        field = MutableStateFlow(currentColorTone)

    val isSaveEnabled = combine(
        selectedColorTone,
        themeManager.hasEditedCustomTheme
    ) { selectedTone, hasEditedCustomTheme ->
        selectedTone != currentColorTone || hasEditedCustomTheme
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun getThemeColors(colorTone: ColorTone): ThemeColors {
        return themeManager.getThemeColors(colorTone)
    }

    fun selectColorTone(colorTone: ColorTone) {
        selectedColorTone.value = colorTone
    }

    fun setColorTone(colorTone: ColorTone) {
        viewModelScope.launch {
            themeManager.setColorTone(colorTone)
            navController.navigateUp()
        }
    }

    fun setPreviewColors(previewColors: ThemeColors) {
        themeManager.setPreviewColors(previewColors)
    }

    fun onColorPicked(semantic: ThemeColorSemantic, colorHexCode: String) {
        themeManager.onColorPicked(semantic, colorHexCode)
    }

    fun shareMyColors() {
        viewModelScope.launch {
            val colorsLink = themeManager.generateCustomColorsLink()
            shareHelper.share(
                title = Res.string.cta_share,
                text = getString(Res.string.menu_share_colors, colorsLink)
            )
        }
    }

    fun unlockPremium() {
        navController.navigate(BookDest.UnlockPremium)
        tracker.logEvent("color_tone_unlock_premium")
    }

    fun highlightShareButton(dest: BubbleDest) {
        viewModelScope.launch { bubbleRepo.addBubbleToQueue(dest) }
    }

    fun processHexFromLink(hexFromLink: String) {
        previousProcessedLink = hexFromLink
        viewModelScope.launch {
            if (themeManager.processHexFromLink(hexFromLink)) {
                snackbarSender.send(Res.string.color_tone_applied_from_link)
            } else {
                snackbarSender.send(Res.string.fallback_error_message)
            }
        }
    }
}