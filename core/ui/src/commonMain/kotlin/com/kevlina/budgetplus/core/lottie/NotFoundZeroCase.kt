package com.kevlina.budgetplus.core.lottie

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.blend
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

private const val BG_DARKEN_FACTOR = 0.5F

@Composable
fun NotFoundZeroCase(modifier: Modifier = Modifier) {

    val composition by rememberLottieComposition { loadLottieSpec("img_empty") }

    val bgColor = LocalAppColors.current.lightBg
    val bgDarkenColor = bgColor.blend(LocalAppColors.current.light, BG_DARKEN_FACTOR)
    val strokeColor = LocalAppColors.current.dark

    @OptIn(ExperimentalCompottieApi::class)
    val dynamicProperties = rememberLottieDynamicProperties {
        shapeLayer("LUPA rotacion 3D") {
            // The X icon
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
            // The outline of the magnifier
            stroke("Group 2", "Stroke 1") {
                color { strokeColor }
            }
            // The grip of the magnifier
            fill("Group 3", "Fill 1") {
                color { strokeColor }
            }
        }

        // The lines in the document
        shapeLayer("**") {
            stroke("Group 1", "Stroke 1") {
                color { strokeColor }
            }
        }

        // Outlines of the document
        shapeLayer("papel bot Outlines") {
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
        }
        shapeLayer("Papel front Outlines") {
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
            stroke("Group 2", "Stroke 1") {
                color { strokeColor }
            }
            // Top-left gradient of the document
            fill("Group 3", "Fill 1") {
                color { bgDarkenColor }
            }
        }
        shapeLayer("Papel top Outlines") {
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
            fill("Group 2", "Fill 1") {
                color { strokeColor }
            }
        }

        // The little circle dot on the right
        shapeLayer("circulito Outlines") {
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
        }
        // The little x on the left
        shapeLayer("x 2 Outlines") {
            fill("Group 1", "Fill 1") {
                color { strokeColor }
            }
        }
        // The big area of the background
        shapeLayer("bg Outlines") {
            fill("Group 1", "Fill 1") {
                color { bgColor }
            }
        }
    }

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            speed = 1.5F,
            dynamicProperties = dynamicProperties
        ),
        contentDescription = null,
        modifier = modifier.size(240.dp)
    )
}

@Preview
@Composable
private fun NotFoundZeroCase_Preview() = AppTheme(themeColors = ThemeColors.Countryside) {
    NotFoundZeroCase()
}