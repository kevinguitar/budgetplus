package com.kevlina.budgetplus.feature.color.tone.picker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_colorize
import com.kevlina.budgetplus.core.theme.ColorTone
import com.kevlina.budgetplus.core.theme.ThemeColorSemantic
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.TOP_BAR_DARKEN_FACTOR
import com.kevlina.budgetplus.core.ui.clickableWithoutRipple
import com.kevlina.budgetplus.core.ui.darken
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun ColorToneCard(
    colorTone: ColorTone,
    themeColors: ThemeColors,
    isLocked: Boolean,
    unlockPremium: () -> Unit,
    onColorPicked: (ThemeColorSemantic, String) -> Unit,
    modifier: Modifier = Modifier,
) {

    var isPickingColor by remember { mutableStateOf<ThemeColorSemantic?>(null) }

    val colors = remember(themeColors) {
        listOf(
            themeColors.light,
            themeColors.lightBg,
            themeColors.primary,
            themeColors.dark,
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {

        ColorToneShowcase(
            colors = colors,
            outlineColor = themeColors.primary.darken(TOP_BAR_DARKEN_FACTOR)
        )

        if (colorTone == ColorTone.Customized) {
            Row(
                horizontalArrangement = Arrangement.Absolute.SpaceAround,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ColorToneConstants.CardCornerRadius))
            ) {
                EditColorButton(color = colors[3]) { isPickingColor = ThemeColorSemantic.Light }
                EditColorButton(color = colors[2]) { isPickingColor = ThemeColorSemantic.LightBg }
                EditColorButton(color = colors[1]) { isPickingColor = ThemeColorSemantic.Primary }
                EditColorButton(color = colors[0]) { isPickingColor = ThemeColorSemantic.Dark }
            }
        }

        if (isLocked) {
            UnlockAnimator(
                color = themeColors.dark,
                modifier = Modifier
                    .size(90.dp)
                    .clickableWithoutRipple(onClick = unlockPremium)
            )
        }
    }

    isPickingColor?.let { semantic ->
        ColorPickerDialog(
            currentColor = when (semantic) {
                ThemeColorSemantic.Light -> themeColors.light
                ThemeColorSemantic.LightBg -> themeColors.lightBg
                ThemeColorSemantic.Primary -> themeColors.primary
                ThemeColorSemantic.Dark -> themeColors.dark
            },
            onColorPicked = { colorHexCode ->
                onColorPicked(semantic, colorHexCode)
                isPickingColor = null
            },
            onDismiss = { isPickingColor = null }
        )
    }
}

@Composable
private fun RowScope.EditColorButton(
    color: Color,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .weight(1F)
            .fillMaxHeight()
            .rippleClick(onClick = onClick)
    ) {

        Icon(
            imageVector = vectorResource(Res.drawable.ic_colorize),
            tint = color,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(16.dp)
        )
    }
}