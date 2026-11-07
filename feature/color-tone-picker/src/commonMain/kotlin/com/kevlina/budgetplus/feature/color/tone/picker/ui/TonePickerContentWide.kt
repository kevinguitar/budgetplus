package com.kevlina.budgetplus.feature.color.tone.picker.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.ColorTone
import com.kevlina.budgetplus.core.theme.ThemeColorSemantic
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.WideContentRow

@Composable
internal fun TonePickerContentWide(
    selectedColorTone: ColorTone,
    pagerState: PagerState,
    isPremium: Boolean,
    getThemeColors: (ColorTone) -> ThemeColors,
    unlockPremium: () -> Unit,
    onColorPicked: (ThemeColorSemantic, String) -> Unit,
) {
    WideContentRow(modifier = Modifier.fillMaxSize()) {
        TonePreview(
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        )

        ColorToneCarousel(
            selectedColorTone = selectedColorTone,
            pagerState = pagerState,
            isPremium = isPremium,
            getThemeColors = getThemeColors,
            unlockPremium = unlockPremium,
            onColorPicked = onColorPicked,
            // The pane next to the preview can be taller than wide, e.g. on the iPhone Duo.
            allowVerticalCards = true,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
                .padding(vertical = 16.dp)
        )
    }
}