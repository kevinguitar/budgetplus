package com.kevlina.budgetplus.feature.color.tone.picker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.color_tone_color_hex_code
import budgetplus.core.common.generated.resources.cta_cancel
import budgetplus.core.common.generated.resources.cta_confirm
import co.touchlab.kermit.Logger
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.convertHexToColor
import com.kevlina.budgetplus.core.theme.toHexCode
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.ButtonRow
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ColorPickerDialog(
    currentColor: Color,
    onColorPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {

    var selectedColor by remember {
        mutableStateOf(ColorEnvelope(
            color = currentColor,
            hexCode = currentColor.toHexCode(),
            fromUser = false
        ))
    }
    val hexCode = rememberTextFieldState("")
    var userInputColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(hexCode.text) {
        val newHexCode = hexCode.text
        // Skip the update if the change comes from the picker
        if (newHexCode == selectedColor.hexCode.drop(2)) {
            return@LaunchedEffect
        }
        hexCode.setTextAndPlaceCursorAtEnd(hexCode.text.toString().trim())
        try {
            userInputColor = hexCode.text.convertHexToColor()
        } catch (e: Exception) {
            Logger.d(e) { "Failed to parse color hex from user input. $newHexCode" }
        }
    }

    AppDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val controller = rememberColorPickerController()

            LaunchedEffect(key1 = userInputColor) {
                userInputColor?.let { controller.selectByColor(it, fromUser = true) }
            }

            HsvColorPicker(
                modifier = Modifier.aspectRatio(1F),
                initialColor = currentColor,
                controller = controller,
                onColorChanged = { envelope: ColorEnvelope ->
                    // do something
                    selectedColor = envelope
                    // Drop the FF which stands for alpha because it's always 1.
                    hexCode.setTextAndPlaceCursorAtEnd(envelope.hexCode.drop(2).uppercase())
                }
            )

            BrightnessSlider(
                controller = controller,
                initialColor = currentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    state = hexCode,
                    title = stringResource(Res.string.color_tone_color_hex_code),
                    modifier = Modifier.weight(1F),
                )

                Spacer(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(AppTheme.cardShape)
                        .background(selectedColor.color)
                )
            }

            ButtonRow {
                Button(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.cta_cancel),
                        color = LocalAppColors.current.light,
                    )
                }

                Button(
                    onClick = { onColorPicked(selectedColor.hexCode) }
                ) {
                    Text(
                        text = stringResource(Res.string.cta_confirm),
                        color = LocalAppColors.current.light,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ColorPickerDialog_Preview() = AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        ColorPickerDialog(Color.Cyan, {}) {}
    }
}